package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.adx.ADXIndicator;

import java.util.List;

/**
 * 趋势强度：ADX(14)，仅用数值不看方向。
 *
 * 阈值：
 * - > 40：强趋势
 * - 25 ~ 40：中等趋势
 * - < 25：无趋势/震荡
 */
public class AdxTrendStrengthFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "trend.adx14_strength";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.TREND_STRUCTURE;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 30) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约30根日线用于 ADX(14)");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        ADXIndicator adx = new ADXIndicator(series, 14);
        double v = adx.getValue(end).doubleValue();

        FactorSignalLevel level;
        String summary;
        if (v > 40) {
            level = FactorSignalLevel.INFO;
            summary = "强趋势行情：更适合趋势跟随，忽略短期超买超卖";
        } else if (v >= 25) {
            level = FactorSignalLevel.NEUTRAL;
            summary = "中等趋势：正常跟随";
        } else {
            level = FactorSignalLevel.WARNING;
            summary = "无趋势/震荡：指标易反复骗线，降低仓位与频率";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("adx14", v)
                .build();
    }
}

