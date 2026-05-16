package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * RSI 极端值：RSI(14)
 *
 * 阈值：
 * - > 85：严重超买（高位终结者：只能卖不能买）
 * - > 70：超买（可持有但不开新仓）
 * - 30 ~ 70：正常
 * - < 30：超卖（可关注）
 * - < 15：极度超卖（若叠加底背离更强）
 */
public class RsiExtremeFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "pattern.rsi14_extreme";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.PATTERN_DIVERGENCE;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 20) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约20根日线用于 RSI(14)");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);
        double v = rsi.getValue(end).doubleValue();

        FactorSignalLevel level;
        String summary;
        if (v > 85) {
            level = FactorSignalLevel.WARNING;
            summary = "RSI>85：严重超买，只能卖不能买";
        } else if (v > 70) {
            level = FactorSignalLevel.WARNING;
            summary = "RSI>70：超买，可持有但不开新仓";
        } else if (v < 15) {
            level = FactorSignalLevel.INFO;
            summary = "RSI<15：极度超卖（若叠加底背离更强）";
        } else if (v < 30) {
            level = FactorSignalLevel.INFO;
            summary = "RSI<30：超卖，关注企稳";
        } else {
            level = FactorSignalLevel.NEUTRAL;
            summary = "RSI正常区间";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("rsi14", v)
                .build();
    }
}

