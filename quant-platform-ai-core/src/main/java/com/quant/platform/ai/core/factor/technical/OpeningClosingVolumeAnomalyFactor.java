package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import com.quant.platform.common.enums.KlineIntervalTypeEnum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 开盘量与尾盘量异常：
 * - 开盘30分钟成交量 / 全天成交量
 * - 尾盘30分钟成交量 / 全天成交量
 *
 * 使用条件：
 * - 当 bars 中为 `interval_type=M1`（1分钟K线）时，可以聚合得到开盘/尾盘30分钟成交量占比。
 */
public class OpeningClosingVolumeAnomalyFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "volume.open_close_30min_anomaly";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.VOLUME_FLOW;
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
        if (!KlineIntervalTypeEnum.M1.getCode().equalsIgnoreCase(bars.get(bars.size() - 1).getIntervalType())) {
            return FactorResult.unavailable(factorKey(), "当前bars不是M1分钟K线，无法计算开盘/尾盘30分钟成交量占比");
        }

        List<KlineBarDTO> dayBars = MinuteKlineDaySlice.latestTradingDayM1(bars);
        if (dayBars.size() < 60) {
            return FactorResult.unavailable(factorKey(), "分钟K线不足60根，无法计算开盘/尾盘30分钟成交量占比");
        }

        LocalDate day = dayBars.get(0).getBarTime().toLocalDate();
        KlineBarDTO prevLast = MinuteKlineDaySlice.previousDayLastBarM1(bars, day);

        long totalVol = sumVolume(dayBars);
        if (totalVol <= 0) {
            return FactorResult.unavailable(factorKey(), "当日总成交量为0，无法计算占比");
        }

        List<KlineBarDTO> open30 = dayBars.subList(0, Math.min(30, dayBars.size()));
        List<KlineBarDTO> tail30 = dayBars.subList(Math.max(0, dayBars.size() - 30), dayBars.size());

        long openVol = sumVolume(open30);
        long tailVol = sumVolume(tail30);
        double openRatio = openVol / (double) totalVol;
        double tailRatio = tailVol / (double) totalVol;

        // 低开/高开：用当日第一根的 open 相对前一交易日最后一根的 close
        Double gapPct = null;
        if (prevLast != null && prevLast.getClose() != null && open30.get(0).getOpen() != null) {
            double prevClose = prevLast.getClose().doubleValue();
            if (prevClose != 0d) {
                gapPct = (open30.get(0).getOpen().doubleValue() - prevClose) / prevClose;
            }
        }

        boolean openRatioHigh = openRatio > 0.35;
        boolean tailRatioHigh = tailRatio > 0.25;

        // 拉尾盘：最后30分钟涨幅（最后close - 尾盘起点close）/尾盘起点close
        double tailStartClose = tail30.get(0).getClose() == null ? 0d : tail30.get(0).getClose().doubleValue();
        double tailEndClose = tail30.get(tail30.size() - 1).getClose() == null ? 0d : tail30.get(tail30.size() - 1).getClose().doubleValue();
        double tailReturn = (tailStartClose == 0d) ? 0d : (tailEndClose - tailStartClose) / tailStartClose;
        boolean pullUpIntoClose = tailReturn > 0.01;

        List<String> notes = new ArrayList<>();
        notes.add("低开/高开使用前一交易日最后一分钟收盘作为参考；若缺数据则gapPct为null");
        notes.add("拉尾盘判定：尾盘30分钟涨幅>1%（可后续参数化）");

        FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        String summary = "开盘/尾盘量占比正常";

        boolean lowOpen = gapPct != null && gapPct < -0.01;
        boolean highOpen = gapPct != null && gapPct > 0.01;

        if (openRatioHigh && lowOpen) {
            level = FactorSignalLevel.INFO;
            summary = "开盘占比>35%且低开：恐慌盘集中出逃，关注日内见底可能";
        } else if (openRatioHigh && highOpen) {
            level = FactorSignalLevel.WARNING;
            summary = "开盘占比>35%且高开：情绪一致性过强，警惕高开低走";
        } else if (tailRatioHigh && pullUpIntoClose) {
            level = FactorSignalLevel.WARNING;
            summary = "尾盘占比>25%且拉尾盘：可能是做收盘价，次日警惕回吐";
        } else if (openRatioHigh || tailRatioHigh) {
            level = FactorSignalLevel.INFO;
            summary = "开盘/尾盘成交量占比偏高：关注资金行为变化";
        }

        FactorResult.Builder b = FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("day", day.toString())
                .metric("total_volume", totalVol)
                .metric("open_30m_volume", openVol)
                .metric("tail_30m_volume", tailVol)
                .metric("open_30m_ratio", openRatio)
                .metric("tail_30m_ratio", tailRatio)
                .metric("tail_return_30m", tailReturn)
                .notes(notes);

        if (gapPct != null) {
            b.metric("gap_open_pct_vs_prev_close", gapPct);
        }

        return b.build();
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
}

