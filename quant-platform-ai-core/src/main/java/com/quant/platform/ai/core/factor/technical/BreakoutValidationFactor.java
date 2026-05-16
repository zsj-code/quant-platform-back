package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;

import java.util.List;

/**
 * 关键价位突破验证（以突破前20日最高价为“关键位”）：
 * - 真突破：当日成交量 > 20日均量的1.5倍，且收盘价站稳在突破线上方 > 1%
 * - 假突破：成交量不足，或收盘击穿后第二日直接跌回（第二日确认需要后续bar，这里只输出“当日是否满足真突破条件”）
 */
public class BreakoutValidationFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "pattern.breakout_validation_20d_high";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.PATTERN_DIVERGENCE;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 30) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约30根日线用于 20日高点与均量判断");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        int start = Math.max(0, end - 20);

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator volMa20 = new SMAIndicator(volume, 20);

        double closeNow = close.getValue(end).doubleValue();
        double volNow = volume.getValue(end).doubleValue();
        double volAvg20 = volMa20.getValue(end).doubleValue();

        double prev20High = Double.NEGATIVE_INFINITY;
        for (int i = start; i <= end - 1; i++) {
            prev20High = Math.max(prev20High, close.getValue(i).doubleValue());
        }

        boolean priceBreak = closeNow > prev20High;
        boolean volOk = volAvg20 > 0 && volNow > 1.5 * volAvg20;
        boolean standAbove = prev20High > 0 && closeNow > prev20High * 1.01;
        boolean trueBreakout = priceBreak && volOk && standAbove;

        FactorSignalLevel level;
        String summary;
        if (trueBreakout) {
            level = FactorSignalLevel.BULLISH;
            summary = "真突破：量能与站稳条件满足";
        } else if (priceBreak) {
            level = FactorSignalLevel.WARNING;
            summary = "突破但未满足量能/站稳条件：警惕假突破";
        } else {
            level = FactorSignalLevel.NEUTRAL;
            summary = "未发生对20日高点的有效突破";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("close_now", closeNow)
                .metric("prev_20d_high", prev20High)
                .metric("volume_now", volNow)
                .metric("volume_ma20", volAvg20)
                .metric("price_break", priceBreak)
                .metric("volume_ok_1_5x", volOk)
                .metric("stand_above_1pct", standAbove)
                .metric("true_breakout", trueBreakout)
                .notes(List.of("假突破的“次日跌回”需要下一根bar确认；此因子仅输出当日真突破条件是否满足"))
                .build();
    }
}

