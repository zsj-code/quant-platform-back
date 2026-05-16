package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import com.quant.platform.common.enums.KlineIntervalTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 日内反转识别（分钟线确认版）：
 * - 长下影线：低点出现在下午，且回升有力，成交量放大 → 日内反转，次日大概率延续反弹。
 * - 长上影线：高点在早盘，随后单边回落，成交量放大 → 拉高出货，次日大概率继续下跌。
 * todo 参数
 */
public class IntradayReversalFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        // 仍保留原key，便于前端/调用方兼容；语义已升级为“分钟线确认版”。
        return "pattern.intraday_reversal_daily_proxy";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.PATTERN_DIVERGENCE;
    }

    @Override
    public String requiredIntervalType() {
        return KlineIntervalTypeEnum.M1.getCode();
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.isEmpty()) {
            return FactorResult.unavailable(factorKey(), "无K线数据");
        }
        KlineBarDTO last = bars.get(bars.size() - 1);
        if (last == null || last.getIntervalType() == null
                || !KlineIntervalTypeEnum.M1.getCode().equalsIgnoreCase(last.getIntervalType())) {
            return FactorResult.unavailable(factorKey(), "当前bars不是M1分钟K线，无法做“高点早盘/低点下午”的日内反转判定");
        }

        List<KlineBarDTO> dayBars = MinuteKlineDaySlice.latestTradingDayM1(bars);
        if (dayBars.size() < 60) {
            return FactorResult.unavailable(factorKey(), "最新交易日分钟K线不足60根，无法做可靠的日内反转判定");
        }
        LocalDate day = dayBars.get(0).getBarTime().toLocalDate();
        List<KlineBarDTO> prevDayBars = previousTradingDayM1(bars, day);

        DayAgg agg = aggregateDay(dayBars);
        if (agg == null) {
            return FactorResult.unavailable(factorKey(), "分钟K线缺少OHLC字段，无法聚合日内形态");
        }

        // 形态：长下影/长上影
        ShadowMetrics shadow = calcShadow(agg);
        if (shadow == null || shadow.range <= 0d) {
            return FactorResult.unavailable(factorKey(), "当日振幅为0或缺字段，无法计算上下影线比例");
        }

        // 时段：低点下午 / 高点早盘
        int lowIdx = indexOfLowestLow(dayBars);
        int highIdx = indexOfHighestHigh(dayBars);
        LocalTime lowTime = lowIdx >= 0 ? dayBars.get(lowIdx).getBarTime().toLocalTime() : null;
        LocalTime highTime = highIdx >= 0 ? dayBars.get(highIdx).getBarTime().toLocalTime() : null;

        boolean lowInAfternoon = lowTime != null && !lowTime.isBefore(LocalTime.of(13, 0));
        boolean highInMorning = highTime != null && !highTime.isAfter(LocalTime.of(11, 30));

        // 回升/回落力度：从低点到收盘、从高点到收盘的回撤/反弹幅度
        Double reboundFromLowPct = (agg.low > 0d) ? (agg.close - agg.low) / agg.low : null;
        Double dropFromHighPct = (agg.high > 0d) ? (agg.high - agg.close) / agg.high : null;
        boolean reboundStrong = reboundFromLowPct != null && reboundFromLowPct >= 0.02;
        boolean dropStrong = dropFromHighPct != null && dropFromHighPct >= 0.02;

        // 量能放大：当日总量相对上一交易日放大（若缺上一日则置为null，不阻断）
        Long dayVol = sumVolume(dayBars);
        Long prevVol = prevDayBars.isEmpty() ? null : sumVolume(prevDayBars);
        Double volRatio = (prevVol != null && prevVol > 0) ? dayVol / (double) prevVol : null;
        boolean volumeAmplified = volRatio != null && volRatio >= 1.20;

        // 长下影线：低点下午 + 回升有力 + 量能放大
        boolean bullish = shadow.longLower && lowInAfternoon && reboundStrong && volumeAmplified;
        // 长上影线：高点早盘 + 单边回落 + 量能放大
        boolean bearish = shadow.longUpper && highInMorning && dropStrong && volumeAmplified;

        FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        String summary = "未触发“分钟线确认版”日内反转";
        if (bullish && !bearish) {
            level = FactorSignalLevel.INFO;
            summary = "长下影线+下午低点+回升有力+放量：日内反转，次日倾向延续反弹";
        } else if (bearish && !bullish) {
            level = FactorSignalLevel.WARNING;
            summary = "长上影线+早盘高点+单边回落+放量：拉高出货，次日倾向继续下跌";
        } else if (bullish) {
            level = FactorSignalLevel.INFO;
            summary = "同时满足多空部分条件（形态/时段信号冲突），建议结合大盘与关键价位确认";
        }

        List<String> notes = new ArrayList<>();
        notes.add("时段阈值：早盘<=11:30；下午>=13:00（A股常见交易时段假设）");
        notes.add("放量阈值：当日量/上一交易日量>=1.20；若缺上一交易日分钟线则volRatio为null且不触发放量条件");
        notes.add("回升/回落力度阈值：从低点到收盘>=2%；从高点到收盘回撤>=2%（后续可参数化）");
        notes.add("长上/下影线：影线占全天振幅>=45% 且 影线>=实体*1.5（后续可参数化）");

        FactorResult.Builder b = FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("day", day.toString())
                .metric("open", agg.open)
                .metric("high", agg.high)
                .metric("low", agg.low)
                .metric("close", agg.close)
                .metric("range", shadow.range)
                .metric("body", shadow.body)
                .metric("lower_shadow", shadow.lowerShadow)
                .metric("upper_shadow", shadow.upperShadow)
                .metric("lower_shadow_ratio", shadow.lowerShadowRatio)
                .metric("upper_shadow_ratio", shadow.upperShadowRatio)
                .metric("long_lower_shadow", shadow.longLower)
                .metric("long_upper_shadow", shadow.longUpper)
                .metric("low_time", lowTime == null ? null : lowTime.toString())
                .metric("high_time", highTime == null ? null : highTime.toString())
                .metric("low_in_afternoon", lowInAfternoon)
                .metric("high_in_morning", highInMorning)
                .metric("rebound_from_low_pct", reboundFromLowPct)
                .metric("drop_from_high_pct", dropFromHighPct)
                .metric("day_volume", dayVol)
                .metric("prev_day_volume", prevVol)
                .metric("volume_ratio_vs_prev_day", volRatio)
                .metric("volume_amplified", volumeAmplified)
                .metric("bullish_intraday_reversal", bullish)
                .metric("bearish_distribution", bearish)
                .notes(notes);

        return b.build();
    }

    private static final class DayAgg {
        final double open;
        final double high;
        final double low;
        final double close;

        private DayAgg(double open, double high, double low, double close) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
        }
    }

    private static final class ShadowMetrics {
        final double range;
        final double body;
        final double lowerShadow;
        final double upperShadow;
        final double lowerShadowRatio;
        final double upperShadowRatio;
        final boolean longLower;
        final boolean longUpper;

        private ShadowMetrics(double range, double body, double lowerShadow, double upperShadow,
                              double lowerShadowRatio, double upperShadowRatio,
                              boolean longLower, boolean longUpper) {
            this.range = range;
            this.body = body;
            this.lowerShadow = lowerShadow;
            this.upperShadow = upperShadow;
            this.lowerShadowRatio = lowerShadowRatio;
            this.upperShadowRatio = upperShadowRatio;
            this.longLower = longLower;
            this.longUpper = longUpper;
        }
    }

    private static DayAgg aggregateDay(List<KlineBarDTO> dayBars) {
        if (dayBars == null || dayBars.isEmpty()) {
            return null;
        }
        KlineBarDTO first = dayBars.get(0);
        KlineBarDTO last = dayBars.get(dayBars.size() - 1);
        if (first == null || last == null) {
            return null;
        }
        if (first.getOpen() == null || last.getClose() == null) {
            return null;
        }
        double open = first.getOpen().doubleValue();
        double close = last.getClose().doubleValue();
        Double high = null;
        Double low = null;
        for (KlineBarDTO b : dayBars) {
            if (b == null || b.getHigh() == null || b.getLow() == null) {
                continue;
            }
            double h = b.getHigh().doubleValue();
            double l = b.getLow().doubleValue();
            high = (high == null) ? h : Math.max(high, h);
            low = (low == null) ? l : Math.min(low, l);
        }
        if (high == null || low == null) {
            return null;
        }
        return new DayAgg(open, high, low, close);
    }

    private static ShadowMetrics calcShadow(DayAgg agg) {
        if (agg == null) {
            return null;
        }
        double range = agg.high - agg.low;
        double body = Math.abs(agg.close - agg.open);
        double lowerShadow = Math.min(agg.open, agg.close) - agg.low;
        double upperShadow = agg.high - Math.max(agg.open, agg.close);
        if (range <= 0d) {
            return new ShadowMetrics(range, body, lowerShadow, upperShadow, 0d, 0d, false, false);
        }
        double lowerRatio = lowerShadow / range;
        double upperRatio = upperShadow / range;

        boolean longLower = lowerRatio >= 0.45 && lowerShadow >= body * 1.5;
        boolean longUpper = upperRatio >= 0.45 && upperShadow >= body * 1.5;

        return new ShadowMetrics(range, body, lowerShadow, upperShadow, lowerRatio, upperRatio, longLower, longUpper);
    }

    private static int indexOfLowestLow(List<KlineBarDTO> dayBars) {
        int idx = -1;
        BigDecimal min = null;
        for (int i = 0; i < dayBars.size(); i++) {
            KlineBarDTO b = dayBars.get(i);
            if (b == null || b.getLow() == null) {
                continue;
            }
            if (min == null || b.getLow().compareTo(min) < 0) {
                min = b.getLow();
                idx = i;
            }
        }
        return idx;
    }

    private static int indexOfHighestHigh(List<KlineBarDTO> dayBars) {
        int idx = -1;
        BigDecimal max = null;
        for (int i = 0; i < dayBars.size(); i++) {
            KlineBarDTO b = dayBars.get(i);
            if (b == null || b.getHigh() == null) {
                continue;
            }
            if (max == null || b.getHigh().compareTo(max) > 0) {
                max = b.getHigh();
                idx = i;
            }
        }
        return idx;
    }

    private static long sumVolume(List<KlineBarDTO> bars) {
        long sum = 0L;
        for (KlineBarDTO b : bars) {
            if (b == null || b.getVolume() == null) {
                continue;
            }
            sum += Math.max(0L, b.getVolume());
        }
        return sum;
    }

    private static List<KlineBarDTO> previousTradingDayM1(List<KlineBarDTO> bars, LocalDate latestDay) {
        if (bars == null || bars.isEmpty() || latestDay == null) {
            return List.of();
        }
        // 从末尾向前找：遇到 < latestDay 的第一天，收集这一整天（升序）
        LocalDate prevDay = null;
        for (int i = bars.size() - 1; i >= 0; i--) {
            KlineBarDTO b = bars.get(i);
            if (b == null || b.getBarTime() == null || b.getIntervalType() == null) {
                continue;
            }
            if (!KlineIntervalTypeEnum.M1.getCode().equalsIgnoreCase(b.getIntervalType())) {
                continue;
            }
            LocalDate d = b.getBarTime().toLocalDate();
            if (d.isBefore(latestDay)) {
                prevDay = d;
                break;
            }
        }
        if (prevDay == null) {
            return List.of();
        }
        // 收集 prevDay 的全部 M1（已是全局升序，直接遍历取即可）
        List<KlineBarDTO> out = new ArrayList<>();
        for (KlineBarDTO b : bars) {
            if (b == null || b.getBarTime() == null || b.getIntervalType() == null) {
                continue;
            }
            if (!KlineIntervalTypeEnum.M1.getCode().equalsIgnoreCase(b.getIntervalType())) {
                continue;
            }
            if (prevDay.equals(b.getBarTime().toLocalDate())) {
                out.add(b);
            }
        }
        return out;
    }
}

