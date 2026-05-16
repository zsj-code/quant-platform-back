package com.quant.platform.ai.core.factor.sentiment.marketwide;

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
 * S3 北向资金 20 日累计净买入（亿元人民币），见 {@code md/情绪面.md}。
 */
public final class S3Northbound20dFlowFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "S3_NORTHBOUND_20D_FLOW";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.MARKET_WIDE;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double netCny100m = null;
        if (netCny100m == null) {
            return missing();
        }
        return classify(netCny100m);
    }

    private FactorResult missing() {
        Map<String, Object> m = thresholdMap();
        return FactorResult.builder(factorKey())
                .level(FactorSignalLevel.UNAVAILABLE)
                .summary("北向 20 日累计净流入：缺少全市场北向日频聚合")
                .metrics(m)
                .notes(List.of("单位：亿元人民币；阈值与 md 文档一致"))
                .build();
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hugeOutflowBelow", SentimentMdThresholds.S3_HUGE_OUTFLOW_BELOW);
        m.put("midOutflowBelow", SentimentMdThresholds.S3_MID_OUTFLOW_BELOW);
        m.put("midInflowAbove", SentimentMdThresholds.S3_MID_INFLOW_ABOVE);
        m.put("hugeInflowAbove", SentimentMdThresholds.S3_HUGE_INFLOW_ABOVE);
        return m;
    }

    public static FactorResult classify(double netBuy20dCny100m) {
        Map<String, Object> m = thresholdMap();
        m.put("netBuy20dCny100m", netBuy20dCny100m);
        if (netBuy20dCny100m < SentimentMdThresholds.S3_HUGE_OUTFLOW_BELOW) {
            return FactorResult.builder("S3_NORTHBOUND_20D_FLOW")
                    .level(FactorSignalLevel.BEARISH)
                    .summary("外资大规模撤离：权重股承压，降低总仓位")
                    .metrics(m)
                    .build();
        }
        if (netBuy20dCny100m < SentimentMdThresholds.S3_MID_OUTFLOW_BELOW) {
            return FactorResult.builder("S3_NORTHBOUND_20D_FLOW")
                    .level(FactorSignalLevel.INFO)
                    .summary("中性偏空")
                    .metrics(m)
                    .build();
        }
        if (netBuy20dCny100m <= SentimentMdThresholds.S3_MID_INFLOW_ABOVE) {
            return FactorResult.builder("S3_NORTHBOUND_20D_FLOW")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("中性")
                    .metrics(m)
                    .build();
        }
        if (netBuy20dCny100m <= SentimentMdThresholds.S3_HUGE_INFLOW_ABOVE) {
            return FactorResult.builder("S3_NORTHBOUND_20D_FLOW")
                    .level(FactorSignalLevel.INFO)
                    .summary("中性偏多")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("S3_NORTHBOUND_20D_FLOW")
                .level(FactorSignalLevel.BULLISH)
                .summary("外资坚决流入：中期行情基础相对扎实，可提高仓位上限")
                .metrics(m)
                .build();
    }
}
