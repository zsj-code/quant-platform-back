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
 * F3 概念/板块轮动速度：每日涨幅前 5 板块次日仍在前 5 的保留率（10 日均，%），见 {@code md/情绪面.md}。
 */
public final class F3SectorRotationRetentionFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "F3_SECTOR_ROTATION_RETENTION";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STYLE_AND_FLOW;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double retention10dPct = null;
        if (retention10dPct == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("板块轮动保留率：缺少板块日涨幅排名与跨日对齐数据")
                    .metrics(m)
                    .notes(List.of("待接入：10 日滚动均值的「前 5 板块次日仍在前 5」保留率"))
                    .build();
        }
        return classify(retention10dPct);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("strongPersistenceAbove", SentimentMdThresholds.F3_STRONG_PERSISTENCE_ABOVE);
        m.put("weakPersistenceBelow", SentimentMdThresholds.F3_WEAK_PERSISTENCE_BELOW);
        return m;
    }

    public static FactorResult classify(double retention10dAvgPct) {
        Map<String, Object> m = thresholdMap();
        m.put("retention10dAvgPct", retention10dAvgPct);
        if (retention10dAvgPct > SentimentMdThresholds.F3_STRONG_PERSISTENCE_ABOVE) {
            return FactorResult.builder("F3_SECTOR_ROTATION_RETENTION")
                    .level(FactorSignalLevel.BULLISH)
                    .summary("主线持续性好：可追涨龙头（注意仓位）")
                    .metrics(m)
                    .build();
        }
        if (retention10dAvgPct >= SentimentMdThresholds.F3_WEAK_PERSISTENCE_BELOW) {
            return FactorResult.builder("F3_SECTOR_ROTATION_RETENTION")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("正常轮动：低吸高抛")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("F3_SECTOR_ROTATION_RETENTION")
                .level(FactorSignalLevel.WARNING)
                .summary("电风扇行情：追高易被套，需管住手")
                .metrics(m)
                .build();
    }
}
