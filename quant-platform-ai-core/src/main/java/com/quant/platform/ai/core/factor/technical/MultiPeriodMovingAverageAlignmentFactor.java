package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * 多周期均线排列（MA20/MA60/MA120）：
 * - 大多头排列：MA20 > MA60 > MA120 且三条均线均向上发散 → 趋势多，只找做多，禁止做空
 * - 大空头排列：MA20 < MA60 < MA120 且三条均线均向下发散 → 趋势空，只找做空/离场，禁止做多
 * - 均线缠绕：三条均线间距狭窄（高低差值 < 价格的3%）→ 无趋势震荡
 */
public class MultiPeriodMovingAverageAlignmentFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "trend.ma_alignment_20_60_120";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.TREND_STRUCTURE;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 130) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约130根日线用于 MA120 与斜率判断");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator ma20 = new SMAIndicator(close, 20);
        SMAIndicator ma60 = new SMAIndicator(close, 60);
        SMAIndicator ma120 = new SMAIndicator(close, 120);

        double c = close.getValue(end).doubleValue();
        double v20 = ma20.getValue(end).doubleValue();
        double v60 = ma60.getValue(end).doubleValue();
        double v120 = ma120.getValue(end).doubleValue();

        // “向上/向下发散”用均线斜率近似：今天值 - 5日前值
        double s20 = v20 - ma20.getValue(end - 5).doubleValue();
        double s60 = v60 - ma60.getValue(end - 5).doubleValue();
        double s120 = v120 - ma120.getValue(end - 5).doubleValue();

        double max = Math.max(v20, Math.max(v60, v120));
        double min = Math.min(v20, Math.min(v60, v120));
        double spreadPct = c == 0 ? 0 : (max - min) / c;

        FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        String summary;

        boolean bull = v20 > v60 && v60 > v120 && s20 > 0 && s60 > 0 && s120 > 0;
        boolean bear = v20 < v60 && v60 < v120 && s20 < 0 && s60 < 0 && s120 < 0;
        boolean tangle = spreadPct < 0.03;

        if (bull) {
            level = FactorSignalLevel.BULLISH;
            summary = "大多头排列：只找做多信号，禁止做空";
        } else if (bear) {
            level = FactorSignalLevel.BEARISH;
            summary = "大空头排列：只找做空/离场信号，禁止做多";
        } else if (tangle) {
            level = FactorSignalLevel.NEUTRAL;
            summary = "均线缠绕：无趋势震荡，切换震荡策略/降低预期";
        } else {
            level = FactorSignalLevel.INFO;
            summary = "均线未形成强一致排列，趋势结构中性";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("close", c)
                .metric("ma20", v20)
                .metric("ma60", v60)
                .metric("ma120", v120)
                .metric("ma20_slope_5d", s20)
                .metric("ma60_slope_5d", s60)
                .metric("ma120_slope_5d", s120)
                .metric("spread_pct_vs_price", spreadPct)
                .build();
    }
}

