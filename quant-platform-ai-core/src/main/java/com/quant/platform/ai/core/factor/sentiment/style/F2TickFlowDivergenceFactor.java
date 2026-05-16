package com.quant.platform.ai.core.factor.sentiment.style;

import com.quant.platform.ai.core.factor.sentiment.SentimentContext;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactor;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactorGroup;
import com.quant.platform.ai.core.factor.sentiment.SentimentMdThresholds;
import com.quant.platform.ai.core.factor.technical.FactorResult;
import com.quant.platform.ai.core.factor.technical.FactorSignalLevel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F2 大单与散户流向背离：近 5 日（特大单+大单净买入 − 中单+小单净买入）/ 总成交额（%），见 {@code md/情绪面.md}。
 */
public final class F2TickFlowDivergenceFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "F2_TICK_FLOW_DIVERGENCE";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STYLE_AND_FLOW;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double diffPct = null;
        if (diffPct == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("大单与散户背离：缺少资金流拆单口径日频数据")
                    .metrics(m)
                    .notes(List.of("待接入：特大单+大单 vs 中小单净买入差值占成交额比例（近 5 日）"))
                    .build();
        }
        return classify(diffPct);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("strongDivergenceAbsAbove", SentimentMdThresholds.F2_STRONG_DIVERGENCE_ABS_ABOVE);
        m.put("flatBandHalfWidth", SentimentMdThresholds.F2_FLAT_BAND_HALF_WIDTH);
        return m;
    }

    public static FactorResult classify(double diffPct) {
        Map<String, Object> m = thresholdMap();
        m.put("diffPct5d", diffPct);
        double w = SentimentMdThresholds.F2_FLAT_BAND_HALF_WIDTH;
        if (diffPct >= -w && diffPct <= w) {
            return FactorResult.builder("F2_TICK_FLOW_DIVERGENCE")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("无明确指引：差值在 ±" + w + "% 带宽内")
                    .metrics(m)
                    .build();
        }
        if (diffPct > SentimentMdThresholds.F2_STRONG_DIVERGENCE_ABS_ABOVE) {
            return FactorResult.builder("F2_TICK_FLOW_DIVERGENCE")
                    .level(FactorSignalLevel.BULLISH)
                    .summary("主力吸筹、散户卖出：差值占比显著为正")
                    .metrics(m)
                    .build();
        }
        if (diffPct < -SentimentMdThresholds.F2_STRONG_DIVERGENCE_ABS_ABOVE) {
            return FactorResult.builder("F2_TICK_FLOW_DIVERGENCE")
                    .level(FactorSignalLevel.BEARISH)
                    .summary("主力出货、散户接盘：差值占比显著为负")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("F2_TICK_FLOW_DIVERGENCE")
                .level(FactorSignalLevel.NEUTRAL)
                .summary("强弱边际：|" + diffPct + "|% 处于 3%~5% 之间，信号不明确")
                .metrics(m)
                .build();
    }
}
