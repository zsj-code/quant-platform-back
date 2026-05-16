package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * 中长期均线斜率：MA60 近5日变化率（%），并做年化（按252交易日近似）。
 *
 * 阈值：
 * - > 5%：强劲上升趋势
 * - 1% ~ 5%：温和上升
 * - -1% ~ 1%：走平
 * - -5% ~ -1%：温和下降
 * - < -5%：强烈下降趋势
 */
public class Ma60AnnualizedSlopeFactor implements TechnicalFactor {
    private static final int TRADING_DAYS_PER_YEAR = 252;

    @Override
    public String factorKey() {
        return "trend.ma60_slope_annualized";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.TREND_STRUCTURE;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 70) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约70根日线用于 MA60 与 5日变化率");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator ma60 = new SMAIndicator(close, 60);

        double now = ma60.getValue(end).doubleValue();
        double prev = ma60.getValue(end - 5).doubleValue();
        if (prev == 0d) {
            return FactorResult.unavailable(factorKey(), "MA60(5日前)为0，无法计算变化率");
        }

        double fiveDayRate = (now - prev) / prev; // ratio
        double annualized = fiveDayRate * (TRADING_DAYS_PER_YEAR / 5.0);
        double annualizedPct = annualized * 100.0;

        FactorSignalLevel level;
        String summary;
        if (annualizedPct > 5) {
            level = FactorSignalLevel.BULLISH;
            summary = "强劲上升趋势：可积极持股";
        } else if (annualizedPct >= 1) {
            level = FactorSignalLevel.INFO;
            summary = "温和上升：正常趋势";
        } else if (annualizedPct > -1) {
            level = FactorSignalLevel.NEUTRAL;
            summary = "走平：趋势可能转变";
        } else if (annualizedPct >= -5) {
            level = FactorSignalLevel.WARNING;
            summary = "温和下降：谨慎";
        } else {
            level = FactorSignalLevel.BEARISH;
            summary = "强烈下降趋势：反弹更偏减仓机会，不抄底";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("ma60_now", now)
                .metric("ma60_5d_ago", prev)
                .metric("five_day_rate", fiveDayRate)
                .metric("annualized_rate", annualized)
                .metric("annualized_pct", annualizedPct)
                .build();
    }
}

