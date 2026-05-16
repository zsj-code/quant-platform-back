package com.quant.platform.business.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.quant.platform.business.kline.entity.KlineBarEntity;
import com.quant.platform.business.kline.mapper.KlineBarMapper;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 基于已落库的 1 分钟 K（{@code interval_type = M1}）聚合生成更高分钟 K（M5/M15/M30/M60/M120），并写入 {@code kline_bar}。
 * <p>
 * 分桶按 A 股日内交易时段拆分为上午/下午两个 session：
 * 上午 09:30～11:30，下午 13:00～15:00。每个 session 以起点为锚点做右闭区间分桶，
 * 例如 60 分钟：09:30～10:30（bar_time=10:30）、10:31～11:30（bar_time=11:30）、13:00～14:00、14:01～15:00。
 * <p>
 * 落库采用“窗口内先删后插”：对每个 symbol+interval 先删除任务窗口 {@code [beg,end]} 内已有分钟 K，再批量插入本次计算结果。
 */
@Service
public class KlineMinuteAggregateService {

    private static final Logger log = LoggerFactory.getLogger(KlineMinuteAggregateService.class);
    private static final String INTERVAL_M1 = KlineIntervalTypeEnum.M1.getCode();
    private static final int INSERT_BATCH_SIZE = 1000;

    private static final Set<KlineIntervalTypeEnum> AGGREGATABLE = EnumSet.of(
            KlineIntervalTypeEnum.M5,
            KlineIntervalTypeEnum.M15,
            KlineIntervalTypeEnum.M30,
            KlineIntervalTypeEnum.M60,
            KlineIntervalTypeEnum.M120);

    private static final LocalTime AM_START = LocalTime.of(9, 30);
    private static final LocalTime AM_END = LocalTime.of(11, 30);
    private static final LocalTime PM_START = LocalTime.of(13, 0);
    private static final LocalTime PM_END = LocalTime.of(15, 0);

    private final StockAdminService stockAdminService;
    private final KlineBarMapper klineBarMapper;

    public KlineMinuteAggregateService(StockAdminService stockAdminService, KlineBarMapper klineBarMapper) {
        this.stockAdminService = stockAdminService;
        this.klineBarMapper = klineBarMapper;
    }

    public long aggregateAll(LocalDate beg, LocalDate end, Set<String> intervalTypeCodes) {
        if (beg == null || end == null || end.isBefore(beg)) {
            return 0L;
        }
        Set<KlineIntervalTypeEnum> targets = resolveTargets(intervalTypeCodes);
        if (targets.isEmpty()) {
            return 0L;
        }
        var stocks = stockAdminService.listNonDelisted();
        if (stocks == null || stocks.isEmpty()) {
            return 0L;
        }
        long affected = 0L;
        for (var s : stocks) {
            if (s == null || s.getCode() == null || s.getCode().trim().isEmpty()) {
                continue;
            }
            String symbol = CommonUtil.toSymbol(s.getCode().trim());
            if (symbol == null || symbol.trim().isEmpty()) {
                continue;
            }
            affected += aggregateForSymbol(symbol.trim(), beg, end, targets);
        }
        return affected;
    }

    public long aggregateForStock(String stockCodeOrSymbol, LocalDate beg, LocalDate end, Set<String> intervalTypeCodes) {
        if (beg == null || end == null || end.isBefore(beg)) {
            return 0L;
        }
        if (stockCodeOrSymbol == null || stockCodeOrSymbol.trim().isEmpty()) {
            return 0L;
        }
        Set<KlineIntervalTypeEnum> targets = resolveTargets(intervalTypeCodes);
        if (targets.isEmpty()) {
            return 0L;
        }
        String trimmed = stockCodeOrSymbol.trim();
        String symbol = trimmed.contains(".") ? trimmed : CommonUtil.toSymbol(trimmed);
        if (symbol == null || symbol.trim().isEmpty()) {
            return 0L;
        }
        return aggregateForSymbol(symbol.trim(), beg, end, targets);
    }

    private long aggregateForSymbol(String symbol, LocalDate beg, LocalDate end, Set<KlineIntervalTypeEnum> targets) {
        LocalDateTime from = beg.atStartOfDay();
        LocalDateTime to = LocalDateTime.of(end, LocalTime.MAX);
        List<KlineBarEntity> m1Asc = loadM1Asc(symbol, from, to);
        if (m1Asc.isEmpty()) {
            return 0L;
        }
        long affected = 0L;
        for (KlineIntervalTypeEnum it : targets) {
            List<KlineBarEntity> aggregated = buildAggregated(m1Asc, symbol, it);
            if (aggregated.isEmpty()) {
                continue;
            }
            affected += persistReplaceWindow(symbol, it.getCode(), beg, end, aggregated);
        }
        return affected;
    }

    private List<KlineBarEntity> loadM1Asc(String symbol, LocalDateTime from, LocalDateTime to) {
        return klineBarMapper.selectList(new LambdaQueryWrapper<KlineBarEntity>()
                .eq(KlineBarEntity::getSymbol, symbol)
                .eq(KlineBarEntity::getIntervalType, INTERVAL_M1)
                .ge(KlineBarEntity::getBarTime, from)
                .le(KlineBarEntity::getBarTime, to)
                .orderByAsc(KlineBarEntity::getBarTime));
    }

    private static Set<KlineIntervalTypeEnum> resolveTargets(Set<String> intervalTypeCodes) {
        if (intervalTypeCodes == null || intervalTypeCodes.isEmpty()) {
            return EnumSet.copyOf(AGGREGATABLE);
        }
        EnumSet<KlineIntervalTypeEnum> out = EnumSet.noneOf(KlineIntervalTypeEnum.class);
        for (String raw : intervalTypeCodes) {
            if (raw == null) {
                continue;
            }
            KlineIntervalTypeEnum t = KlineIntervalTypeEnum.fromCode(raw.trim());
            if (t != null && AGGREGATABLE.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<KlineBarEntity> buildAggregated(List<KlineBarEntity> m1Asc, String symbol, KlineIntervalTypeEnum it) {
        int minutes = intervalMinutes(it);
        if (minutes <= 1) {
            return List.of();
        }

        Map<String, List<KlineBarEntity>> buckets = new LinkedHashMap<>();
        for (KlineBarEntity b : m1Asc) {
            if (b == null || b.getBarTime() == null) {
                continue;
            }
            LocalDateTime t = b.getBarTime();
            LocalTime lt = t.toLocalTime();
            LocalTime sessionStart = sessionStartOf(lt);
            if (sessionStart == null) {
                continue;
            }
            long offset = ChronoUnit.MINUTES.between(sessionStart, lt);
            if (offset < 0) {
                continue;
            }
            long idx = (offset == 0) ? 0 : (offset - 1) / minutes;
            String key = String.format(Locale.ROOT, "%s|%s|%d", t.toLocalDate(), sessionStart, idx);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(b);
        }

        List<KlineBarEntity> out = new ArrayList<>(buckets.size());
        for (List<KlineBarEntity> bucket : buckets.values()) {
            KlineBarEntity e = collapseBucket(bucket, symbol, it.getCode());
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    private static int intervalMinutes(KlineIntervalTypeEnum it) {
        if (it == null) {
            return -1;
        }
        switch (it) {
            case M5:
                return 5;
            case M15:
                return 15;
            case M30:
                return 30;
            case M60:
                return 60;
            case M120:
                return 120;
            default:
                return -1;
        }
    }

    private static LocalTime sessionStartOf(LocalTime t) {
        if (t == null) {
            return null;
        }
        if (!t.isBefore(AM_START) && !t.isAfter(AM_END)) {
            return AM_START;
        }
        if (!t.isBefore(PM_START) && !t.isAfter(PM_END)) {
            return PM_START;
        }
        return null;
    }

    private static KlineBarEntity collapseBucket(List<KlineBarEntity> bucket, String symbol, String intervalCode) {
        if (bucket == null || bucket.isEmpty()) {
            return null;
        }
        KlineBarEntity first = bucket.get(0);
        KlineBarEntity last = bucket.get(bucket.size() - 1);
        if (first == null || last == null || first.getBarTime() == null || last.getBarTime() == null) {
            return null;
        }

        BigDecimal high = null;
        BigDecimal low = null;
        long volume = 0L;
        BigDecimal amountSum = null;
        for (KlineBarEntity b : bucket) {
            if (b == null) {
                continue;
            }
            high = maxNullable(high, b.getHigh());
            low = minNullable(low, b.getLow());
            volume += b.getVolume() == null ? 0L : b.getVolume();
            if (b.getAmount() != null) {
                amountSum = amountSum == null ? b.getAmount() : amountSum.add(b.getAmount());
            }
        }

        KlineBarEntity e = new KlineBarEntity();
        e.setSymbol(symbol);
        e.setIntervalType(intervalCode);
        e.setBarTime(last.getBarTime());
        e.setOpen(first.getOpen());
        e.setHigh(high);
        e.setLow(low);
        e.setClose(last.getClose());
        e.setVolume(volume);
        e.setAmount(amountSum);
        return e;
    }

    private static BigDecimal maxNullable(BigDecimal a, BigDecimal b) {
        if (b == null) {
            return a;
        }
        if (a == null) {
            return b;
        }
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static BigDecimal minNullable(BigDecimal a, BigDecimal b) {
        if (b == null) {
            return a;
        }
        if (a == null) {
            return b;
        }
        return a.compareTo(b) <= 0 ? a : b;
    }

    private long persistReplaceWindow(String symbol, String intervalCode, LocalDate beg, LocalDate end,
            List<KlineBarEntity> computed) {
        if (computed == null || computed.isEmpty()) {
            return 0L;
        }
        LocalDateTime from = beg.atStartOfDay();
        LocalDateTime to = LocalDateTime.of(end, LocalTime.MAX);

        klineBarMapper.delete(new LambdaQueryWrapper<KlineBarEntity>()
                .eq(KlineBarEntity::getSymbol, symbol)
                .eq(KlineBarEntity::getIntervalType, intervalCode)
                .ge(KlineBarEntity::getBarTime, from)
                .le(KlineBarEntity::getBarTime, to));

        for (KlineBarEntity c : computed) {
            c.setId(IdWorker.getIdStr());
        }

        long inserted = 0L;
        for (int i = 0; i < computed.size(); i += INSERT_BATCH_SIZE) {
            int j = Math.min(i + INSERT_BATCH_SIZE, computed.size());
            klineBarMapper.insertBatch(computed.subList(i, j));
            inserted += j - i;
        }
        log.debug("kline minute persist symbol={} interval={} inserted={}", symbol, intervalCode, inserted);
        return inserted;
    }
}

