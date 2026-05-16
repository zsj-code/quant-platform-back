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
 * S1 全市场恐慌-贪婪综合指数（0~100 标准化分，见 {@code md/情绪面.md}）。
 */
public final class S1MarketPanicGreedFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "S1_MARKET_PANIC_GREED";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.MARKET_WIDE;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double composite = null;
        if (composite == null) {
            return missing();
        }
        return classify(composite);
    }

    private FactorResult missing() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("extremeFearBelow", SentimentMdThresholds.S1_EXTREME_FEAR_BELOW);
        m.put("fearHigh", SentimentMdThresholds.S1_FEAR_HIGH);
        m.put("neutralHigh", SentimentMdThresholds.S1_NEUTRAL_HIGH);
        m.put("greedyHigh", SentimentMdThresholds.S1_GREEDY_HIGH);
        return FactorResult.builder(factorKey())
                .level(FactorSignalLevel.UNAVAILABLE)
                .summary("全市场恐慌-贪婪：缺少标准化综合指数（0~100）")
                .metrics(m)
                .notes(List.of("待接入：50日上涨占比、全市场成交量偏离、沪深300期现价差、融资买入占比、北向净流入偏离等，加权标准化后写入本因子输入"))
                .build();
    }

    /** 接入数据后直接调用。 */
    public static FactorResult classify(double composite0to100) {
        double x = composite0to100;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("composite0to100", x);
        m.put("extremeFearBelow", SentimentMdThresholds.S1_EXTREME_FEAR_BELOW);
        m.put("fearHigh", SentimentMdThresholds.S1_FEAR_HIGH);
        m.put("neutralHigh", SentimentMdThresholds.S1_NEUTRAL_HIGH);
        m.put("greedyHigh", SentimentMdThresholds.S1_GREEDY_HIGH);
        if (x < SentimentMdThresholds.S1_EXTREME_FEAR_BELOW) {
            return FactorResult.builder("S1_MARKET_PANIC_GREED")
                    .level(FactorSignalLevel.WARNING)
                    .summary("极度恐慌：可考虑左侧分批建仓信号（未必立刻反转）")
                    .metrics(m)
                    .build();
        }
        if (x < SentimentMdThresholds.S1_FEAR_HIGH) {
            return FactorResult.builder("S1_MARKET_PANIC_GREED")
                    .level(FactorSignalLevel.INFO)
                    .summary("偏恐慌：低仓位观望")
                    .metrics(m)
                    .build();
        }
        if (x < SentimentMdThresholds.S1_NEUTRAL_HIGH) {
            return FactorResult.builder("S1_MARKET_PANIC_GREED")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("中性：正常交易")
                    .metrics(m)
                    .build();
        }
        if (x < SentimentMdThresholds.S1_GREEDY_HIGH) {
            return FactorResult.builder("S1_MARKET_PANIC_GREED")
                    .level(FactorSignalLevel.WARNING)
                    .summary("偏贪婪：逐步止盈减仓")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("S1_MARKET_PANIC_GREED")
                .level(FactorSignalLevel.WARNING)
                .summary("极度贪婪：限制新开仓，仅平仓或对冲")
                .metrics(m)
                .build();
    }
}
