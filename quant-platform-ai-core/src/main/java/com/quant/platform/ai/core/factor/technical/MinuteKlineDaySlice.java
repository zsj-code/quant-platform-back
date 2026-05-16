package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class MinuteKlineDaySlice {
    private MinuteKlineDaySlice() {
    }

    static List<KlineBarDTO> latestTradingDayM1(List<KlineBarDTO> bars) {
        if (bars == null || bars.isEmpty()) {
            return List.of();
        }
        // 假定 bars 已按时间升序；从最后一根开始向前收集同一天的 M1
        KlineBarDTO last = bars.get(bars.size() - 1);
        if (last == null || last.getBarTime() == null) {
            return List.of();
        }
        LocalDate day = last.getBarTime().toLocalDate();
        List<KlineBarDTO> dayBars = new ArrayList<>();
        for (int i = bars.size() - 1; i >= 0; i--) {
            KlineBarDTO b = bars.get(i);
            if (b == null || b.getBarTime() == null) {
                continue;
            }
            if (!day.equals(b.getBarTime().toLocalDate())) {
                break;
            }
            if (!"M1".equalsIgnoreCase(b.getIntervalType())) {
                // 同一天混入非M1就跳过（不终止收集）
                continue;
            }
            dayBars.add(b);
        }
        // 目前是倒序，翻转为升序
        List<KlineBarDTO> asc = new ArrayList<>(dayBars.size());
        for (int i = dayBars.size() - 1; i >= 0; i--) {
            asc.add(dayBars.get(i));
        }
        return asc;
    }

    static KlineBarDTO previousDayLastBarM1(List<KlineBarDTO> bars, LocalDate latestDay) {
        if (bars == null || bars.isEmpty() || latestDay == null) {
            return null;
        }
        for (int i = bars.size() - 1; i >= 0; i--) {
            KlineBarDTO b = bars.get(i);
            if (b == null || b.getBarTime() == null) {
                continue;
            }
            if (!"M1".equalsIgnoreCase(b.getIntervalType())) {
                continue;
            }
            if (b.getBarTime().toLocalDate().isBefore(latestDay)) {
                return b; // 这是上一交易日的最后一根（因为全局升序）
            }
        }
        return null;
    }
}

