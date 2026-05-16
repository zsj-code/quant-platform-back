package com.quant.platform.business.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.quant.platform.business.kline.entity.KlineBarEntity;
import com.quant.platform.business.kline.mapper.KlineBarMapper;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;

/**
 * 基于已落库的日 K（{@code interval_type = D}）聚合生成周 / 月 / 年 K，并写入 {@code kline_bar}。
 * <p>
 * 规则：周期内 open=首日开盘、high/low=最高/最低、close=末日收盘、volume=成交量之和；
 * {@code bar_time} 为该周期内<strong>最后一个交易日</strong>的 00:00:00（与日 K 一致）。
 * 周线按 ISO 周（{@link IsoFields}）划分。
 * <p>
 * 拉取日 K 时，起点会按本次选中的周期向前扩：含 {@code beg} 所在 ISO 周的周一、{@code beg} 所在月 1 日、{@code beg}
 * 所在年 1 月 1 日（按当前聚合类型分别扩窗）；落库仅写入「周期日历区间」与任务窗口 {@code [beg,end]} 有交集的 K 线
 * （不要求周期末日落在窗口内，避免退市等导致末日偏早而丢根）。
 * <p>
 * 落库时对同一 symbol、同一周期先删后插：年/月按<strong>公历</strong>年、月匹配；周按「周一 00:00～周日结束」时间窗
 * （与 {@link IsoFields} 周划分、{@link #isoWeekKey} 在 Java 默认 ISO 日历下一致），避免同一周期多条 {@code bar_time}。
 */
@Service
public class KlineDailyAggregateService {

    private static final Logger log = LoggerFactory.getLogger(KlineDailyAggregateService.class);
    private static final String INTERVAL_DAILY = KlineIntervalTypeEnum.D.getCode();
    private static final int INSERT_BATCH_SIZE = 500;

    private static final Set<KlineIntervalTypeEnum> AGGREGATABLE = EnumSet.of(
            KlineIntervalTypeEnum.W,
            KlineIntervalTypeEnum.M,
            KlineIntervalTypeEnum.Y);

    private final StockAdminService stockAdminService;
    private final KlineBarMapper klineBarMapper;

    public KlineDailyAggregateService(StockAdminService stockAdminService, KlineBarMapper klineBarMapper) {
        this.stockAdminService = stockAdminService;
        this.klineBarMapper = klineBarMapper;
    }

    /**
     * @param beg              日 K 起始日期（含）
     * @param end              日 K 结束日期（含）
     * @param intervalTypeCodes 仅处理其中的 W、M、Y（大小写不敏感），其它忽略
     * @return 本次落库的 K 线根数（先按周期删除再插入，等价于周期级替换）
     */
    public long aggregateAll(LocalDate beg, LocalDate end, Set<String> intervalTypeCodes) {
        if (beg == null || end == null || end.isBefore(beg)) {
            return 0L;
        }
        Set<KlineIntervalTypeEnum> targets = resolveTargets(intervalTypeCodes);
        if (targets.isEmpty()) {
            return 0L;
        }

        List<StockEntity> stocks = stockAdminService.listNonDelisted();
        if (stocks == null || stocks.isEmpty()) {
            return 0L;
        }

        long affected = 0L;
        for (StockEntity s : stocks) {
            if (s == null) {
                continue;
            }
            String code = s.getCode();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            String symbol = CommonUtil.toSymbol(code.trim());
            if (symbol == null || symbol.trim().isEmpty()) {
                continue;
            }
            affected += aggregateForSymbol(symbol.trim(), beg, end, targets);
        }
        return affected;
    }

    /**
     * 仅对一只股票聚合。{@code stockCodeOrSymbol} 可为 6 位证券代码（如 {@code 600000}），或已带交易所后缀的
     * symbol（如 {@code 600000.SH}），与 {@link #aggregateAll} 规则一致。
     *
     * @return 本次落库的 K 线根数（先按周期删除再插入）
     */
    public long aggregateForStock(String stockCodeOrSymbol, LocalDate beg, LocalDate end,
            Set<String> intervalTypeCodes) {
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
        long affected = 0L;
        for (KlineIntervalTypeEnum it : targets) {
            LocalDate loadStart = expandedDailyLoadStart(beg, it);
            LocalDateTime from = loadStart.atStartOfDay();
            LocalDateTime to = LocalDateTime.of(end, LocalTime.MAX);
            List<KlineBarEntity> dailies = loadDailyAsc(symbol, from, to);
            if (dailies.isEmpty()) {
                continue;
            }
            affected += aggregateOneInterval(symbol, it, dailies, beg, end);
        }
        return affected;
    }

    /**
     * 日 K 查询下界：在 {@code beg} 之前尽可能前移，以覆盖首根周/月/年 K 所需的完整自然周、自然月、自然年内的日 K。
     */
    private static LocalDate expandedDailyLoadStart(LocalDate beg, KlineIntervalTypeEnum interval) {
        LocalDate start = beg;
        if (interval == KlineIntervalTypeEnum.W) {
            LocalDate monday = beg.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (monday.isBefore(start)) {
                start = monday;
            }
        }
        if (interval == KlineIntervalTypeEnum.M) {
            LocalDate monthFirst = beg.withDayOfMonth(1);
            if (monthFirst.isBefore(start)) {
                start = monthFirst;
            }
        }
        if (interval == KlineIntervalTypeEnum.Y) {
            LocalDate yearFirst = LocalDate.of(beg.getYear(), 1, 1);
            if (yearFirst.isBefore(start)) {
                start = yearFirst;
            }
        }
        return start;
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

    private List<KlineBarEntity> loadDailyAsc(String symbol, LocalDateTime from, LocalDateTime to) {
        return klineBarMapper.selectList(new LambdaQueryWrapper<KlineBarEntity>().eq(KlineBarEntity::getSymbol, symbol)
                .eq(KlineBarEntity::getIntervalType, INTERVAL_DAILY).ge(KlineBarEntity::getBarTime, from)
                .le(KlineBarEntity::getBarTime, to).orderByAsc(KlineBarEntity::getBarTime));
    }

    private long aggregateOneInterval(String symbol, KlineIntervalTypeEnum interval, List<KlineBarEntity> dailyAsc,
            LocalDate outputBeg, LocalDate outputEnd) {
        Function<LocalDate, String> keyFn;
        switch (interval) {
            case W:
                keyFn = KlineDailyAggregateService::isoWeekKey;
                break;
            case M:
                keyFn = KlineDailyAggregateService::monthKey;
                break;
            case Y:
                keyFn = KlineDailyAggregateService::yearKey;
                break;
            default:
                return 0L;
        }

        List<KlineBarEntity> aggregated = buildAggregated(dailyAsc, symbol, interval.getCode(), keyFn);
        aggregated = filterAggregatedByOutputWindow(aggregated, interval, outputBeg, outputEnd);
        if (aggregated.isEmpty()) {
            return 0L;
        }
        return persist(symbol, interval.getCode(), aggregated);
    }

    /**
     * 仅保留「周期日历区间」与任务窗口 {@code [outputBeg,outputEnd]} 有交集的 K 线。
     * <p>
     * 退市等情况下，周期内最后交易日可能早于 {@code outputBeg}（如扩窗后 4 月根 K 的末日为 4 月 10 日而任务从 4 月 15
     * 日起算），按末日落在窗口内会误丢该根历史 K；按周期相交可保留。
     */
    private static List<KlineBarEntity> filterAggregatedByOutputWindow(List<KlineBarEntity> bars,
            KlineIntervalTypeEnum interval, LocalDate outputBeg, LocalDate outputEnd) {
        if (bars == null || bars.isEmpty()) {
            return List.of();
        }
        List<KlineBarEntity> out = new ArrayList<>();
        for (KlineBarEntity b : bars) {
            if (b == null || b.getBarTime() == null) {
                continue;
            }
            LocalDate lastTrade = b.getBarTime().toLocalDate();
            if (periodCalendarIntersectsJobWindow(lastTrade, interval, outputBeg, outputEnd)) {
                out.add(b);
            }
        }
        return out;
    }

    /**
     * @param lastTradeInPeriod 该周/月/年内最后一根日 K 的日期（与 {@code bar_time} 一致）
     */
    private static boolean periodCalendarIntersectsJobWindow(LocalDate lastTradeInPeriod, KlineIntervalTypeEnum interval,
            LocalDate outputBeg, LocalDate outputEnd) {
        if (lastTradeInPeriod == null || outputBeg == null || outputEnd == null || outputEnd.isBefore(outputBeg)) {
            return false;
        }
        LocalDate periodStart;
        LocalDate periodEnd;
        switch (interval) {
            case W:
                periodStart = lastTradeInPeriod.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                periodEnd = periodStart.plusDays(6);
                break;
            case M:
                YearMonth ym = YearMonth.from(lastTradeInPeriod);
                periodStart = ym.atDay(1);
                periodEnd = ym.atEndOfMonth();
                break;
            case Y:
                periodStart = LocalDate.of(lastTradeInPeriod.getYear(), 1, 1);
                periodEnd = LocalDate.of(lastTradeInPeriod.getYear(), 12, 31);
                break;
            default:
                return false;
        }
        return !periodStart.isAfter(outputEnd) && !periodEnd.isBefore(outputBeg);
    }

    private static List<KlineBarEntity> buildAggregated(List<KlineBarEntity> dailyAsc, String symbol, String intervalCode,
            Function<LocalDate, String> periodKey) {
        List<KlineBarEntity> out = new ArrayList<>();
        List<KlineBarEntity> bucket = new ArrayList<>();
        String current = null;

        for (KlineBarEntity d : dailyAsc) {
            if (d == null || d.getBarTime() == null) {
                continue;
            }
            LocalDate day = d.getBarTime().toLocalDate();
            String k = periodKey.apply(day);
            if (current == null) {
                current = k;
            } else if (!current.equals(k)) {
                KlineBarEntity bar = collapseBucket(bucket, symbol, intervalCode);
                if (bar != null) {
                    out.add(bar);
                }
                bucket.clear();
                current = k;
            }
            bucket.add(d);
        }
        KlineBarEntity lastBar = collapseBucket(bucket, symbol, intervalCode);
        if (lastBar != null) {
            out.add(lastBar);
        }
        return out;
    }

    private static String isoWeekKey(LocalDate d) {
        int y = d.get(IsoFields.WEEK_BASED_YEAR);
        int w = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format(Locale.ROOT, "%04d-W%02d", y, w);
    }

    private static String monthKey(LocalDate d) {
        return YearMonth.from(d).toString();
    }

    private static String yearKey(LocalDate d) {
        return String.format(Locale.ROOT, "%04d", d.getYear());
    }

    private static KlineBarEntity collapseBucket(List<KlineBarEntity> bucket, String symbol, String intervalCode) {
        if (bucket.isEmpty()) {
            return null;
        }
        KlineBarEntity first = bucket.get(0);
        KlineBarEntity last = bucket.get(bucket.size() - 1);
        if (first.getBarTime() == null || last.getBarTime() == null) {
            return null;
        }

        BigDecimal high = null;
        BigDecimal low = null;
        long volume = 0L;
        BigDecimal amountSum = null;
        BigDecimal turnoverRateSum = null;
        for (KlineBarEntity b : bucket) {
            high = maxNullable(high, b.getHigh());
            low = minNullable(low, b.getLow());
            volume += b.getVolume() == null ? 0L : b.getVolume();
            if (b.getAmount() != null) {
                amountSum = amountSum == null ? b.getAmount() : amountSum.add(b.getAmount());
            }
            if (b.getTurnoverRate() != null) {
                turnoverRateSum = turnoverRateSum == null? b.getTurnoverRate() : turnoverRateSum.add(b.getTurnoverRate());
            }
        }

        KlineBarEntity e = new KlineBarEntity();
        e.setSymbol(symbol);
        e.setIntervalType(intervalCode);
        LocalDate endDay = last.getBarTime().toLocalDate();
        e.setBarTime(endDay.atStartOfDay());
        e.setOpen(first.getOpen());
        e.setHigh(high);
        e.setLow(low);
        e.setClose(last.getClose());
        e.setVolume(volume);
        e.setAmount(amountSum);
        e.setTurnoverRate(turnoverRateSum);
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

    /**
     * 同一自然周期仅保留一行：先删该 symbol+interval 下同周期内所有旧 {@code bar_time}，再插入本次计算结果。
     */
    private long persist(String symbol, String intervalCode, List<KlineBarEntity> computed) {
        if (computed == null || computed.isEmpty()) {
            return 0L;
        }
        KlineIntervalTypeEnum it = KlineIntervalTypeEnum.fromCode(intervalCode);
        if (it == null || !AGGREGATABLE.contains(it)) {
            return 0L;
        }

        Map<String, KlineBarEntity> byPeriod = new LinkedHashMap<>();
        for (KlineBarEntity c : computed) {
            if (c == null || c.getBarTime() == null) {
                continue;
            }
            LocalDate d = c.getBarTime().toLocalDate();
            byPeriod.put(periodDedupKey(d, it), c);
        }
        List<KlineBarEntity> rows = new ArrayList<>(byPeriod.values());
        if (rows.isEmpty()) {
            return 0L;
        }

        Set<String> periodsCleared = new HashSet<>();
        for (KlineBarEntity c : rows) {
            LocalDate d = c.getBarTime().toLocalDate();
            String pk = periodDedupKey(d, it);
            if (periodsCleared.add(pk)) {
                deleteBarsInSameCalendarPeriod(symbol, intervalCode, d, it);
            }
        }

        for (KlineBarEntity c : rows) {
            c.setId(IdWorker.getIdStr());
        }
        long inserted = 0L;
        for (int i = 0; i < rows.size(); i += INSERT_BATCH_SIZE) {
            int end = Math.min(i + INSERT_BATCH_SIZE, rows.size());
            klineBarMapper.insertBatch(rows.subList(i, end));
            inserted += end - i;
        }
        log.debug("kline persist symbol={} interval={} periodsCleared={} inserted={}", symbol, intervalCode,
                periodsCleared.size(), inserted);
        return inserted;
    }

    private static String periodDedupKey(LocalDate d, KlineIntervalTypeEnum it) {
        switch (it) {
            case W:
                return isoWeekKey(d);
            case M:
                return monthKey(d);
            case Y:
                return yearKey(d);
            default:
                return d.toString();
        }
    }

    private void deleteBarsInSameCalendarPeriod(String symbol, String intervalCode, LocalDate anchorDay,
            KlineIntervalTypeEnum it) {
        var w = new LambdaQueryWrapper<KlineBarEntity>().eq(KlineBarEntity::getSymbol, symbol)
                .eq(KlineBarEntity::getIntervalType, intervalCode);
        switch (it) {
            case Y:
                w.apply("YEAR(bar_time) = {0}", anchorDay.getYear());
                break;
            case M:
                YearMonth ym = YearMonth.from(anchorDay);
                w.apply("YEAR(bar_time) = {0} AND MONTH(bar_time) = {1}", ym.getYear(), ym.getMonthValue());
                break;
            case W:
                LocalDate mon = anchorDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sun = mon.plusDays(6);
                w.ge(KlineBarEntity::getBarTime, mon.atStartOfDay())
                        .le(KlineBarEntity::getBarTime, LocalDateTime.of(sun, LocalTime.MAX));
                break;
            default:
                return;
        }
        klineBarMapper.delete(w);
    }
}
