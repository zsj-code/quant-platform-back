package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * MACD 顶/底背离（近似实现）：
 * - 顶背离：股价创20日新高，但 MACD 柱峰值未创新高，且 DIFF 走平/下降 → 减仓
 * - 底背离：股价创20日新低，但 MACD 柱谷底未创新低，且 DIFF 走平/上升 → 建仓
 *
 * 说明：
 * - 这里用 ta4j 计算 DIFF(MACD line=EMA12-EMA26) 与 DEA(signal=EMA9 of DIFF)，柱=DIFF-DEA
 * - “背离”按近20日窗口内：当前价创新高/新低，且柱值未同步创新高/新低 来近似。
 */
public class MacdDivergenceFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "pattern.macd_divergence_20d";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.PATTERN_DIVERGENCE;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 60) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约60根日线用于 MACD 与 20日背离检测");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        int start = Math.max(0, end - 19);

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        MACDIndicator diff = new MACDIndicator(close, 12, 26);
        EMAIndicator dea = new EMAIndicator(diff, 9);

        double closeNow = close.getValue(end).doubleValue();
        double diffNow = diff.getValue(end).doubleValue();
        double histNow = diffNow - dea.getValue(end).doubleValue();

        double maxClose = Double.NEGATIVE_INFINITY;
        double minClose = Double.POSITIVE_INFINITY;
        double maxHist = Double.NEGATIVE_INFINITY;
        double minHist = Double.POSITIVE_INFINITY;

        for (int i = start; i <= end; i++) {
            double c = close.getValue(i).doubleValue();
            double h = diff.getValue(i).doubleValue() - dea.getValue(i).doubleValue();
            maxClose = Math.max(maxClose, c);
            minClose = Math.min(minClose, c);
            maxHist = Math.max(maxHist, h);
            minHist = Math.min(minHist, h);
        }

        boolean priceNewHigh20 = closeNow >= maxClose;
        boolean priceNewLow20 = closeNow <= minClose;

        // 顶背离：价创新高，但柱未创高（当前柱 < 窗口柱最大值 * 0.95 视为“明显未创新高”）
        boolean topDiv = priceNewHigh20 && histNow < maxHist * 0.95 && diffNow <= diff.getValue(end - 1).doubleValue();
        // 底背离：价创新低，但柱未创新低（当前柱 > 窗口柱最小值 * 0.95；注意柱可能为负，这里用相对幅度近似）
        boolean bottomDiv = priceNewLow20 && histNow > minHist * 0.95 && diffNow >= diff.getValue(end - 1).doubleValue();

        FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        String summary = "未检测到典型 MACD 顶/底背离(20日窗口近似)";
        if (topDiv) {
            level = FactorSignalLevel.WARNING;
            summary = "MACD 顶背离：上涨动能衰竭，偏减仓信号";
        } else if (bottomDiv) {
            level = FactorSignalLevel.BULLISH;
            summary = "MACD 底背离：下跌动能衰竭，偏建仓信号";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("close_now", closeNow)
                .metric("close_max20", maxClose)
                .metric("close_min20", minClose)
                .metric("diff_now", diffNow)
                .metric("dea_now", dea.getValue(end).doubleValue())
                .metric("hist_now", histNow)
                .metric("hist_max20", maxHist)
                .metric("hist_min20", minHist)
                .metric("price_new_high_20", priceNewHigh20)
                .metric("price_new_low_20", priceNewLow20)
                .metric("top_divergence", topDiv)
                .metric("bottom_divergence", bottomDiv)
                .notes(List.of("背离实现为近似：严格的峰谷匹配（两次峰值/谷底对应）需要后续增强峰谷识别"))
                .build();
    }
}

