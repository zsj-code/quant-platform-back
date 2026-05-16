package com.quant.platform.business.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.quant.platform.business.client.EastmoneyKlineClient;
import com.quant.platform.business.kline.dto.EastmoneyKlineBarDTO;
import com.quant.platform.business.kline.entity.KlineBarEntity;
import com.quant.platform.business.kline.entity.KlineSyncLogEntity;
import com.quant.platform.business.kline.mapper.KlineBarMapper;
import com.quant.platform.business.kline.mapper.KlineSyncLogMapper;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class KlineSyncService {

    /** 单次批量插入行数上限，避免 SQL 过长或超过 MySQL max_allowed_packet */
    private static final int INSERT_BATCH_SIZE = 500;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Logger log = LoggerFactory.getLogger(KlineSyncService.class);

    private final StockAdminService stockAdminService;
    private final EastmoneyKlineClient eastmoneyKlineClient;
    private final KlineBarMapper klineBarMapper;
    private final KlineSyncLogMapper klineSyncLogMapper;

    public KlineSyncService(StockAdminService stockAdminService, EastmoneyKlineClient eastmoneyKlineClient,
            KlineBarMapper klineBarMapper, KlineSyncLogMapper klineSyncLogMapper) {
        this.stockAdminService = stockAdminService;
        this.eastmoneyKlineClient = eastmoneyKlineClient;
        this.klineBarMapper = klineBarMapper;
        this.klineSyncLogMapper = klineSyncLogMapper;
    }

    /**
     * @param intervalType
     *            周期码（须能被 {@link KlineIntervalTypeEnum#fromCode(String)} 识别）
     */
    public long syncAll(String intervalType, int fqt, LocalDate beg, LocalDate end, int sleepMsPerStock) {
        KlineIntervalTypeEnum it = KlineIntervalTypeEnum.fromCode(intervalType);
        if (it == null) {
            return 0L;
        }
        String intervalCode = it.getCode();
        int useKlt = it.getEastmoneyKlt();

        List<StockEntity> stocks = stockAdminService.listNonDelisted();
        long inserted = 0L;
        if (stocks == null || stocks.isEmpty()) {
            return 0L;
        }
        LocalDate syncRunDate = LocalDate.now();
        int processStock = 0;
        Random random = new Random();
        int num = random.nextInt(4) + 4;
        for (StockEntity s : stocks) {
            String code = s == null ? null : s.getCode();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }

            String symbol = CommonUtil.toSymbol(code);
            String secid = CommonUtil.toSecId(code);
            if (secid == null) {
                continue;
            }

            if (alreadySyncedToday(symbol, intervalCode, syncRunDate)) {
                log.info("stockCode = {} has synced today", symbol);
                continue;
            }

            List<EastmoneyKlineBarDTO> bars = eastmoneyKlineClient.fetchKline(secid, useKlt, fqt, beg, end);
            long n = upsertBars(symbol, intervalCode, bars);
            inserted += n;
            processStock++;
            recordSyncLog(code.trim(), symbol, intervalCode, syncRunDate, n);

            if (sleepMsPerStock > 0) {
                try {
                     if (processStock % 30 == 0) {
                        Thread.sleep(7 * 1000L);
                    } else {
                        Thread.sleep(sleepMsPerStock);
                    }
//                    Thread.sleep(sleepMsPerStock);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return inserted;
                }
            }
        }

        return inserted;
    }

    private boolean alreadySyncedToday(String symbol, String intervalType, LocalDate syncDate) {
        if (symbol == null || symbol.trim().isEmpty() || intervalType == null || intervalType.trim().isEmpty()
                || syncDate == null) {
            return false;
        }
        return klineSyncLogMapper.selectCount(new LambdaQueryWrapper<KlineSyncLogEntity>()
                .eq(KlineSyncLogEntity::getSymbol, symbol).eq(KlineSyncLogEntity::getIntervalType, intervalType)
                .eq(KlineSyncLogEntity::getSyncDate, syncDate)) > 0;
    }

    private void recordSyncLog(String stockCode, String symbol, String intervalType, LocalDate syncDate, long insertedBars) {
        if (stockCode == null || stockCode.trim().isEmpty() || symbol == null || symbol.trim().isEmpty()
                || intervalType == null || intervalType.trim().isEmpty() || syncDate == null) {
            return;
        }
        KlineSyncLogEntity row = new KlineSyncLogEntity();
        row.setId(IdWorker.getIdStr());
        row.setStockCode(stockCode.trim());
        row.setSymbol(symbol.trim());
        row.setIntervalType(intervalType.trim());
        row.setSyncDate(syncDate);
        int c = insertedBars > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) insertedBars;
        row.setBarCount(c);
        row.setCreatedAt(LocalDateTime.now());
        klineSyncLogMapper.insertIgnore(row);
    }

    public long syncAllMulti(Set<String> intervalTypes, int fqt, LocalDate beg, LocalDate end, int sleepMsPerStock) {
        if (intervalTypes == null || intervalTypes.isEmpty()) {
            return 0L;
        }
        long inserted = 0L;
        for (String t : intervalTypes) {
            KlineIntervalTypeEnum it = KlineIntervalTypeEnum.fromCode(t);
            if (it == null) {
                continue;
            }
            inserted += syncAll(it.getCode(), fqt, beg, end, sleepMsPerStock);
        }
        return inserted;
    }

    public static Map<String, String> parseJobParam(String raw) {
        if (raw == null) {
            return Map.of();
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new HashMap<>();
        String[] pairs = v.split("[;&\\n]");
        for (String p : pairs) {
            if (p == null) {
                continue;
            }
            String s = p.trim();
            if (s.isEmpty()) {
                continue;
            }
            int idx = s.indexOf('=');
            if (idx <= 0 || idx == s.length() - 1) {
                continue;
            }
            String k = s.substring(0, idx).trim();
            String val = s.substring(idx + 1).trim();
            if (!k.isEmpty() && !val.isEmpty()) {
                out.put(k, val);
            }
        }
        return out;
    }

    public static LocalDate parseDate(String s, LocalDate defaultValue) {
        if (s == null || s.trim().isEmpty()) {
            return defaultValue;
        }
        String v = s.trim();
        if (v.length() >= 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
            try {
                return LocalDate.parse(v.substring(0, 10), DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (v.length() >= 8) {
            String yyyy = v.substring(0, 4);
            String mm = v.substring(4, 6);
            String dd = v.substring(6, 8);
            try {
                return LocalDate.parse(yyyy + "-" + mm + "-" + dd, DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return defaultValue;
    }

    public static Set<String> parseIntervalTypes(String s) {
        if (s == null || s.trim().isEmpty()) {
            return Set.of();
        }
        Set<String> out = new HashSet<>();
        for (String p : s.split("[,|]")) {
            if (p == null) {
                continue;
            }
            String v = p.trim();
            if (!v.isEmpty()) {
                out.add(v.toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private long upsertBars(String symbol, String intervalType, List<EastmoneyKlineBarDTO> bars) {
        if (symbol == null || symbol.trim().isEmpty() || intervalType == null || intervalType.trim().isEmpty()
                || bars == null || bars.isEmpty()) {
            return 0L;
        }

        LocalDateTime min = null;
        LocalDateTime max = null;
        Set<LocalDateTime> incomingTimes = new HashSet<>();
        for (EastmoneyKlineBarDTO b : bars) {
            LocalDateTime t = parseBarTime(b == null ? null : b.getDate());
            if (t == null) {
                continue;
            }
            incomingTimes.add(t);
            if (min == null || t.isBefore(min)) {
                min = t;
            }
            if (max == null || t.isAfter(max)) {
                max = t;
            }
        }
        if (incomingTimes.isEmpty() || min == null || max == null) {
            return 0L;
        }

        List<KlineBarEntity> existing = klineBarMapper
                .selectList(new LambdaQueryWrapper<KlineBarEntity>().select(KlineBarEntity::getBarTime)
                        .eq(KlineBarEntity::getSymbol, symbol).eq(KlineBarEntity::getIntervalType, intervalType)
                        .ge(KlineBarEntity::getBarTime, min).le(KlineBarEntity::getBarTime, max));
        Set<LocalDateTime> exists = new HashSet<>();
        if (existing != null) {
            for (KlineBarEntity e : existing) {
                if (e != null && e.getBarTime() != null) {
                    exists.add(e.getBarTime());
                }
            }
        }

        List<KlineBarEntity> toInsert = new ArrayList<>();
        for (EastmoneyKlineBarDTO b : bars) {
            if (b == null) {
                continue;
            }
            LocalDateTime t = parseBarTime(b.getDate());
            if (t == null || exists.contains(t)) {
                continue;
            }

            KlineBarEntity entity = new KlineBarEntity();
            entity.setId(IdWorker.getIdStr());
            entity.setSymbol(symbol);
            entity.setIntervalType(intervalType);
            entity.setBarTime(t);
            entity.setOpen(b.getOpen());
            entity.setHigh(b.getHigh());
            entity.setLow(b.getLow());
            entity.setClose(b.getClose());
            entity.setVolume(b.getVolume());
            entity.setAmount(b.getAmount());
            entity.setAmplitude(b.getAmplitude());
            entity.setChangePct(b.getChangePct());
            entity.setChangeAmount(b.getChangeAmount());
            entity.setTurnoverRate(b.getTurnoverRate());
            toInsert.add(entity);
        }

        if (toInsert.isEmpty()) {
            return 0L;
        }

        for (int i = 0; i < toInsert.size(); i += INSERT_BATCH_SIZE) {
            int end = Math.min(i + INSERT_BATCH_SIZE, toInsert.size());
            klineBarMapper.insertBatch(toInsert.subList(i, end));
        }
        return toInsert.size();
    }

    private static LocalDateTime parseBarTime(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String v = s.trim();
        if (v.contains(" ")) {
            try {
                return LocalDateTime.parse(v, DATE_TIME_SEC);
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDateTime.parse(v, DATE_TIME_MIN);
            } catch (DateTimeParseException ignored) {
            }
            return null;
        }
        try {
            return LocalDate.parse(v, DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

}
