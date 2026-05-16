package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KlineAggregationUtils {
    private KlineAggregationUtils() {
    }

    /**
     * 将 M1 分钟K聚合为“日线”K（按 bar_time 的 LocalDate 分组）。
     * 约定：
     * - open：当日第一根分钟K open
     * - close：当日最后一根分钟K close
     * - high：当日最高 high
     * - low：当日最低 low
     * - volume：当日 volume 求和
     * - bar_time：当日最后一根分钟K的 bar_time
     * - interval_type：D1
     */
    public static List<KlineBarDTO> aggregateM1ToD1(List<KlineBarDTO> m1BarsAsc) {
        if (m1BarsAsc == null || m1BarsAsc.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<KlineBarDTO>> byDay = new LinkedHashMap<>();
        for (KlineBarDTO b : m1BarsAsc) {
            if (b == null || b.getBarTime() == null) {
                continue;
            }
            if (b.getIntervalType() != null && !"M1".equalsIgnoreCase(b.getIntervalType())) {
                continue;
            }
            byDay.computeIfAbsent(b.getBarTime().toLocalDate(), k -> new ArrayList<>()).add(b);
        }

        List<KlineBarDTO> out = new ArrayList<>(byDay.size());
        for (Map.Entry<LocalDate, List<KlineBarDTO>> e : byDay.entrySet()) {
            List<KlineBarDTO> day = e.getValue();
            if (day.isEmpty()) {
                continue;
            }
            KlineBarDTO first = day.get(0);
            KlineBarDTO last = day.get(day.size() - 1);

            BigDecimal open = first.getOpen();
            BigDecimal close = last.getClose();
            BigDecimal high = null;
            BigDecimal low = null;
            long volSum = 0L;

            for (KlineBarDTO b : day) {
                if (b.getHigh() != null) {
                    high = high == null ? b.getHigh() : high.max(b.getHigh());
                }
                if (b.getLow() != null) {
                    low = low == null ? b.getLow() : low.min(b.getLow());
                }
                if (b.getVolume() != null) {
                    volSum += Math.max(0L, b.getVolume());
                }
            }

            KlineBarDTO d1 = new KlineBarDTO();
            d1.setSymbol(first.getSymbol());
            d1.setIntervalType("D1");
            d1.setBarTime(last.getBarTime());
            d1.setOpen(open);
            d1.setClose(close);
            d1.setHigh(high);
            d1.setLow(low);
            d1.setVolume(volSum);
            out.add(d1);
        }

        return out;
    }
}

