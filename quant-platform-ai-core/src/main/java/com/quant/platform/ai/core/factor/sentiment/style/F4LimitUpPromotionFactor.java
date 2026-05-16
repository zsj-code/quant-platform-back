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
 * F4 涨停晋级率：昨日首板中今日晋级二板比例（剔除一字板，%），见 {@code md/情绪面.md}。
 */
public final class F4LimitUpPromotionFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "F4_LIMIT_UP_PROMOTION";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STYLE_AND_FLOW;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double promotionPct = null;
        if (promotionPct == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("涨停晋级率：缺少全市场首板/二板与一字剔除标注数据")
                    .metrics(m)
                    .notes(List.of("待接入：昨日首板集合、今日二板成功集合、一字板剔除规则"))
                    .build();
        }
        return classify(promotionPct);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("excellentAbove", SentimentMdThresholds.F4_EXCELLENT_ABOVE);
        m.put("normalHigh", SentimentMdThresholds.F4_NORMAL_HIGH);
        m.put("weakHigh", SentimentMdThresholds.F4_WEAK_HIGH);
        return m;
    }

    public static FactorResult classify(double promotionPct) {
        Map<String, Object> m = thresholdMap();
        m.put("promotionPct", promotionPct);
        if (promotionPct > SentimentMdThresholds.F4_EXCELLENT_ABOVE) {
            return FactorResult.builder("F4_LIMIT_UP_PROMOTION")
                    .level(FactorSignalLevel.BULLISH)
                    .summary("极高：打板环境极佳，短线情绪高潮")
                    .metrics(m)
                    .build();
        }
        if (promotionPct >= SentimentMdThresholds.F4_NORMAL_HIGH) {
            return FactorResult.builder("F4_LIMIT_UP_PROMOTION")
                    .level(FactorSignalLevel.INFO)
                    .summary("正常偏强：短线可操作")
                    .metrics(m)
                    .build();
        }
        if (promotionPct >= SentimentMdThresholds.F4_WEAK_HIGH) {
            return FactorResult.builder("F4_LIMIT_UP_PROMOTION")
                    .level(FactorSignalLevel.WARNING)
                    .summary("偏弱：题材持续性不足")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("F4_LIMIT_UP_PROMOTION")
                .level(FactorSignalLevel.BEARISH)
                .summary("冰点：短线生态恶劣，停止打板接力策略")
                .metrics(m)
                .build();
    }
}
