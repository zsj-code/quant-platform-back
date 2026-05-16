package com.quant.platform.ai.core.factor.sentiment.stock;

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
 * G2 个股舆情情感：积极帖 / 消极帖（滚动 3 日），见 {@code md/情绪面.md}。
 */
public final class G2StockSocialSentimentFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "G2_STOCK_SOCIAL_SENTIMENT";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STOCK_SPECIFIC;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double posNegRatio3d = null;
        if (posNegRatio3d == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("个股舆情情感：缺少帖子情感标注与 3 日滚动聚合")
                    .metrics(m)
                    .notes(List.of("依赖 G1 有效帖子集合"))
                    .build();
        }
        return classify(posNegRatio3d);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("euphoriaAbove", SentimentMdThresholds.G2_EUPHORIA_ABOVE);
        m.put("optimisticHigh", SentimentMdThresholds.G2_OPTIMISTIC_HIGH);
        m.put("debateLow", SentimentMdThresholds.G2_DEBATE_LOW);
        m.put("pessimisticLow", SentimentMdThresholds.G2_PESSIMISTIC_LOW);
        return m;
    }

    public static FactorResult classify(double positiveOverNegativeRatio3d) {
        Map<String, Object> m = thresholdMap();
        m.put("posNegRatio3d", positiveOverNegativeRatio3d);
        if (positiveOverNegativeRatio3d > SentimentMdThresholds.G2_EUPHORIA_ABOVE) {
            return FactorResult.builder("G2_STOCK_SOCIAL_SENTIMENT")
                    .level(FactorSignalLevel.WARNING)
                    .summary("一边倒看多：一致预期常为反向指标")
                    .metrics(m)
                    .build();
        }
        if (positiveOverNegativeRatio3d >= SentimentMdThresholds.G2_OPTIMISTIC_HIGH) {
            return FactorResult.builder("G2_STOCK_SOCIAL_SENTIMENT")
                    .level(FactorSignalLevel.INFO)
                    .summary("偏乐观")
                    .metrics(m)
                    .build();
        }
        if (positiveOverNegativeRatio3d >= SentimentMdThresholds.G2_DEBATE_LOW) {
            return FactorResult.builder("G2_STOCK_SOCIAL_SENTIMENT")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("多空分歧，正常")
                    .metrics(m)
                    .build();
        }
        if (positiveOverNegativeRatio3d >= SentimentMdThresholds.G2_PESSIMISTIC_LOW) {
            return FactorResult.builder("G2_STOCK_SOCIAL_SENTIMENT")
                    .level(FactorSignalLevel.INFO)
                    .summary("偏悲观")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("G2_STOCK_SOCIAL_SENTIMENT")
                .level(FactorSignalLevel.INFO)
                .summary("极度悲观：若股价不再创新低，常为潜在反转结构（需价量确认）")
                .metrics(m)
                .build();
    }
}
