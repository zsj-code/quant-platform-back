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
 * G3 帖子生存周期：近 24h 内发布后 1h 内被删帖占比（%），见 {@code md/情绪面.md}。
 */
public final class G3PostDeleteRatioFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "G3_POST_DELETE_RATIO";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STOCK_SPECIFIC;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double deletedWithin1hRatioPct = null;
        if (deletedWithin1hRatioPct == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("删帖率：缺少帖子存活/删除事件流")
                    .metrics(m)
                    .build();
        }
        return classify(deletedWithin1hRatioPct);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("manipulationSuspicionAbove", SentimentMdThresholds.G3_MANIPULATION_SUSPICION_ABOVE);
        return m;
    }

    public static FactorResult classify(double deletedWithin1hRatioPct24h) {
        Map<String, Object> m = thresholdMap();
        m.put("deletedWithin1hRatioPct24h", deletedWithin1hRatioPct24h);
        if (deletedWithin1hRatioPct24h > SentimentMdThresholds.G3_MANIPULATION_SUSPICION_ABOVE) {
            return FactorResult.builder("G3_POST_DELETE_RATIO")
                    .level(FactorSignalLevel.WARNING)
                    .summary("舆论操控嫌疑：信息环境不真实，系统应回避该标的（无论多空）")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("G3_POST_DELETE_RATIO")
                .level(FactorSignalLevel.NEUTRAL)
                .summary("删帖率未超警戒：未发现明显舆论操控迹象")
                .metrics(m)
                .build();
    }
}
