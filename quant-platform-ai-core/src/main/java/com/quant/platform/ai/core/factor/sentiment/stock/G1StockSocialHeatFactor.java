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
 * G1 个股舆情热度：近 24h 新增帖数 / 20 日均（去机器人后），见 {@code md/情绪面.md}。
 */
public final class G1StockSocialHeatFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "G1_STOCK_SOCIAL_HEAT";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STOCK_SPECIFIC;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double ratioVs20dAvg = null;
        if (ratioVs20dAvg == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("个股舆情热度：缺少多源帖子计数（东财/雪球/同花顺）及机器人过滤结果")
                    .metrics(m)
                    .notes(List.of("symbol=" + ctx.getSymbol()))
                    .build();
        }
        return classify(ratioVs20dAvg);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("anomalyBurstAbove", SentimentMdThresholds.G1_ANOMALY_BURST_ABOVE);
        m.put("warmHigh", SentimentMdThresholds.G1_WARM_HIGH);
        m.put("coldBelow", SentimentMdThresholds.G1_COLD_BELOW);
        return m;
    }

    public static FactorResult classify(double postCount24hOverAvg20d) {
        Map<String, Object> m = thresholdMap();
        m.put("ratioVs20dAvg", postCount24hOverAvg20d);
        if (postCount24hOverAvg20d > SentimentMdThresholds.G1_ANOMALY_BURST_ABOVE) {
            return FactorResult.builder("G1_STOCK_SOCIAL_HEAT")
                    .level(FactorSignalLevel.WARNING)
                    .summary("异常爆量：可能机器人或重大消息扩散，需结合 G3 删帖率判断")
                    .metrics(m)
                    .build();
        }
        if (postCount24hOverAvg20d >= SentimentMdThresholds.G1_WARM_HIGH) {
            return FactorResult.builder("G1_STOCK_SOCIAL_HEAT")
                    .level(FactorSignalLevel.INFO)
                    .summary("显著升温：人气上升，技术信号可信度增强")
                    .metrics(m)
                    .build();
        }
        if (postCount24hOverAvg20d >= SentimentMdThresholds.G1_COLD_BELOW) {
            return FactorResult.builder("G1_STOCK_SOCIAL_HEAT")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("正常波动")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("G1_STOCK_SOCIAL_HEAT")
                .level(FactorSignalLevel.INFO)
                .summary("无人问津：技术金叉等信号可能缺乏资金跟进")
                .metrics(m)
                .build();
    }
}
