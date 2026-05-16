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
 * S2 50ETF 期权 PCR（认沽/认购成交量比，5 日均），见 {@code md/情绪面.md}。
 */
public final class S2OptionPcr50EtfFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "S2_OPTION_PCR_50ETF";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.MARKET_WIDE;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        Double pcr5d = null;
        if (pcr5d == null) {
            return missing();
        }
        return classify(pcr5d);
    }

    private FactorResult missing() {
        Map<String, Object> m = thresholdMap();
        return FactorResult.builder(factorKey())
                .level(FactorSignalLevel.UNAVAILABLE)
                .summary("50ETF 期权 PCR：缺少认沽/认购成交量（5 日均）")
                .metrics(m)
                .notes(List.of("待接入：全市场 50ETF 相关期权成交量聚合或官方 PCR 序列"))
                .build();
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("extremePanicAbove", SentimentMdThresholds.S2_EXTREME_PANIC_ABOVE);
        m.put("bearishHigh", SentimentMdThresholds.S2_BEARISH_HIGH);
        m.put("neutralHigh", SentimentMdThresholds.S2_NEUTRAL_HIGH);
        m.put("bullishLow", SentimentMdThresholds.S2_BULLISH_LOW);
        return m;
    }

    public static FactorResult classify(double pcr) {
        Map<String, Object> m = thresholdMap();
        m.put("pcr5d", pcr);
        if (pcr > SentimentMdThresholds.S2_EXTREME_PANIC_ABOVE) {
            return FactorResult.builder("S2_OPTION_PCR_50ETF")
                    .level(FactorSignalLevel.INFO)
                    .summary("极度恐慌：机构大量买保险，常为市场底部领先信号")
                    .metrics(m)
                    .build();
        }
        if (pcr >= SentimentMdThresholds.S2_BEARISH_HIGH) {
            return FactorResult.builder("S2_OPTION_PCR_50ETF")
                    .level(FactorSignalLevel.INFO)
                    .summary("偏恐慌")
                    .metrics(m)
                    .build();
        }
        if (pcr >= SentimentMdThresholds.S2_NEUTRAL_HIGH) {
            return FactorResult.builder("S2_OPTION_PCR_50ETF")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("中性")
                    .metrics(m)
                    .build();
        }
        if (pcr >= SentimentMdThresholds.S2_BULLISH_LOW) {
            return FactorResult.builder("S2_OPTION_PCR_50ETF")
                    .level(FactorSignalLevel.INFO)
                    .summary("偏贪婪")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("S2_OPTION_PCR_50ETF")
                .level(FactorSignalLevel.WARNING)
                .summary("极度贪婪：机构对冲意愿弱，下跌风险增大")
                .metrics(m)
                .build();
    }
}
