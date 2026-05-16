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
 * F1 北向资金行业偏好度：近 10 日净买入最多的申万一级行业中，最大行业占北向总净买入比重（%），见 {@code md/情绪面.md}。
 */
public final class F1NorthIndustryPreferenceFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "F1_NORTH_INDUSTRY_PREFERENCE";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STYLE_AND_FLOW;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double maxIndustrySharePct = null;
        if (maxIndustrySharePct == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("北向行业偏好：缺少分行业净流入与 Top 行业占比序列")
                    .metrics(m)
                    .notes(List.of("待接入：北向近 10 日分申万一级行业净买入，取最大行业占比"))
                    .build();
        }
        return classify(maxIndustrySharePct);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("highlyConcentratedAbove", SentimentMdThresholds.F1_HIGHLY_CONCENTRATED_ABOVE);
        m.put("dispersedMaxShareBelow", SentimentMdThresholds.F1_DISPERSED_MAX_SHARE_BELOW);
        return m;
    }

    public static FactorResult classify(double maxIndustryShareAmongTopFlowsPct) {
        Map<String, Object> m = thresholdMap();
        m.put("maxIndustrySharePct", maxIndustryShareAmongTopFlowsPct);
        if (maxIndustryShareAmongTopFlowsPct > SentimentMdThresholds.F1_HIGHLY_CONCENTRATED_ABOVE) {
            return FactorResult.builder("F1_NORTH_INDUSTRY_PREFERENCE")
                    .level(FactorSignalLevel.BULLISH)
                    .summary("外资高度一致主攻单一方向：该方向回调时支撑或更强")
                    .metrics(m)
                    .build();
        }
        if (maxIndustryShareAmongTopFlowsPct < SentimentMdThresholds.F1_DISPERSED_MAX_SHARE_BELOW) {
            return FactorResult.builder("F1_NORTH_INDUSTRY_PREFERENCE")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("外资无主线（分散）：市场大概率震荡")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("F1_NORTH_INDUSTRY_PREFERENCE")
                .level(FactorSignalLevel.NEUTRAL)
                .summary("行业偏好中等集中")
                .metrics(m)
                .build();
    }
}
