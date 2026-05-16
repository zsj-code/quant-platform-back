package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

/**
 * 价格与MA200距离：(Close - MA200) / MA200 * 100%
 *
 * 阈值：
 * - > 30%：极度超涨
 * - 15% ~ 30%：偏超买（可持有但不开新仓）
 * - -10% ~ 15%：正常
 * - -20% ~ -10%：偏超卖（关注企稳）
 * - < -20%：极度超跌（左侧可分批试仓，但需放量阳线确认——确认逻辑需要额外信号）
 */
public class PriceMa200DistanceFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "trend.price_ma200_distance_pct";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.TREND_STRUCTURE;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 210) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约210根日线用于 MA200");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator ma200 = new SMAIndicator(close, 200);

        double c = close.getValue(end).doubleValue();
        double m = ma200.getValue(end).doubleValue();
        if (m == 0d) {
            return FactorResult.unavailable(factorKey(), "MA200为0，无法计算距离");
        }

        double distPct = (c - m) / m * 100.0;

        FactorSignalLevel level;
        String summary;
        if (distPct > 30) {
            level = FactorSignalLevel.WARNING;
            summary = "极度超涨：均值回归风险极高，谨慎追高";
        } else if (distPct >= 15) {
            level = FactorSignalLevel.WARNING;
            summary = "偏超买：可持有但不建议开新仓";
        } else if (distPct >= -10) {
            level = FactorSignalLevel.NEUTRAL;
            summary = "正常区间";
        } else if (distPct >= -20) {
            level = FactorSignalLevel.INFO;
            summary = "偏超卖：关注企稳信号";
        } else {
            level = FactorSignalLevel.INFO;
            summary = "极度超跌：可关注分批试仓，但需放量阳线确认（该确认需额外因子配合）";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("close", c)
                .metric("ma200", m)
                .metric("distance_pct", distPct)
                .build();
    }
}

