package com.quant.platform.ai.core.factor.sentiment.derivatives;

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
 * D1 个股近月平值期权隐含波动率在其一年历史中的分位（0~100），见 {@code md/情绪面.md}。
 */
public final class D1StockOptionIvRankFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "D1_STOCK_OPTION_IV_RANK";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.DERIVATIVES_AND_SHADOW;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double ivPercentile1y = null;
        if (ivPercentile1y == null) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("个股 IV 分位：缺少近月平值 IV 与一年历史分位计算")
                    .metrics(m)
                    .notes(List.of("symbol=" + ctx.getSymbol()))
                    .build();
        }
        return classify(ivPercentile1y);
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ivHighStressPct", SentimentMdThresholds.D1_IV_HIGH_STRESS_PCT);
        m.put("ivLowCalmPct", SentimentMdThresholds.D1_IV_LOW_CALM_PCT);
        return m;
    }

    public static FactorResult classify(double ivPercentileRank0to100) {
        Map<String, Object> m = thresholdMap();
        m.put("ivPercentile1y", ivPercentileRank0to100);
        if (ivPercentileRank0to100 > SentimentMdThresholds.D1_IV_HIGH_STRESS_PCT) {
            return FactorResult.builder("D1_STOCK_OPTION_IV_RANK")
                    .level(FactorSignalLevel.WARNING)
                    .summary("IV 高位（>90% 分位）：不确定性极高，波动将加剧，方向不明")
                    .metrics(m)
                    .build();
        }
        if (ivPercentileRank0to100 < SentimentMdThresholds.D1_IV_LOW_CALM_PCT) {
            return FactorResult.builder("D1_STOCK_OPTION_IV_RANK")
                    .level(FactorSignalLevel.INFO)
                    .summary("IV 低位（<10% 分位）：波动预期极低；卖方胜率偏高但需防变盘")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("D1_STOCK_OPTION_IV_RANK")
                .level(FactorSignalLevel.NEUTRAL)
                .summary("IV 处于一年区间中段")
                .metrics(m)
                .build();
    }
}
