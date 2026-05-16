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
 * D3 券商研报标题情绪：唱多词密度环比（%）及近月覆盖情况，见 {@code md/情绪面.md}。
 */
public final class D3BrokerReportTitleSentimentFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "D3_BROKER_REPORT_TITLE_SENTIMENT";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.DERIVATIVES_AND_SHADOW;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        return missing(ctx);
    }

    private static FactorResult missing(SentimentContext ctx) {
        Map<String, Object> m = thresholdMap();
        return FactorResult.builder("D3_BROKER_REPORT_TITLE_SENTIMENT")
                .level(FactorSignalLevel.UNAVAILABLE)
                .summary("研报标题情绪：缺少近月/上月标题抓取与情感、密度统计")
                .metrics(m)
                .notes(List.of("symbol=" + ctx.getSymbol()))
                .build();
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bullishTitleDensityMomSurgePct", SentimentMdThresholds.D3_BULLISH_TITLE_DENSITY_MOM_SURGE_PCT);
        return m;
    }

    /**
     * @param bullishTitleDensityMomPct 「强烈推荐」「上调」等词密度环比（%，可为 null 表示无法计算）
     * @param noBullishCoverageLastMonth 近 1 个月无研报或均为中性/无法识别唱多
     */
    public static FactorResult classify(Double bullishTitleDensityMomPct, boolean noBullishCoverageLastMonth) {
        Map<String, Object> m = thresholdMap();
        m.put("bullishTitleDensityMomPct", bullishTitleDensityMomPct);
        m.put("noBullishCoverageLastMonth", noBullishCoverageLastMonth);
        if (noBullishCoverageLastMonth) {
            return FactorResult.builder("D3_BROKER_REPORT_TITLE_SENTIMENT")
                    .level(FactorSignalLevel.INFO)
                    .summary("近月无研报覆盖或均为中性：关注度低，需警惕基本面长期弱化可能")
                    .metrics(m)
                    .build();
        }
        if (bullishTitleDensityMomPct == null) {
            return FactorResult.builder("D3_BROKER_REPORT_TITLE_SENTIMENT")
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("有研报但缺少环比密度计算结果")
                    .metrics(m)
                    .build();
        }
        if (bullishTitleDensityMomPct > SentimentMdThresholds.D3_BULLISH_TITLE_DENSITY_MOM_SURGE_PCT) {
            return FactorResult.builder("D3_BROKER_REPORT_TITLE_SENTIMENT")
                    .level(FactorSignalLevel.WARNING)
                    .summary("唱多词密度环比大增：常为出货流动性反向指标")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("D3_BROKER_REPORT_TITLE_SENTIMENT")
                .level(FactorSignalLevel.NEUTRAL)
                .summary("研报标题情绪未触发文档极端档")
                .metrics(m)
                .build();
    }
}
