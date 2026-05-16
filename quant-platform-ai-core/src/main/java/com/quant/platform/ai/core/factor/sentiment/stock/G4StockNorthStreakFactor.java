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
 * G4 北向资金个股流入纪律：近 20 日连续净流入/流出天数与累计占流通市值（%），见 {@code md/情绪面.md}。
 */
public final class G4StockNorthStreakFactor implements SentimentFactor {

    @Override
    public String factorKey() {
        return "G4_STOCK_NORTH_STREAK";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.STOCK_SPECIFIC;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        return missing(ctx);
    }

    private static FactorResult missing(SentimentContext ctx) {
        Map<String, Object> m = thresholdMap();
        return FactorResult.builder("G4_STOCK_NORTH_STREAK")
                .level(FactorSignalLevel.UNAVAILABLE)
                .summary("北向个股纪律：缺少个股北向日频净流入与流通市值口径")
                .metrics(m)
                .notes(List.of("symbol=" + ctx.getSymbol()))
                .build();
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("streakDays", SentimentMdThresholds.G4_STREAK_DAYS);
        m.put("cumulativeMvPct", SentimentMdThresholds.G4_CUMULATIVE_MV_PCT);
        return m;
    }

    /**
     * @param consecutiveInDays    连续净流入天数（无则 0）
     * @param consecutiveOutDays   连续净流出天数（无则 0）
     * @param cumulativeInMvPct    与连续流入段对齐的累计净流入占流通市值 %
     * @param cumulativeOutMvPct   与连续流出段对齐的累计净流出占流通市值 %（正数）
     */
    public static FactorResult classify(int consecutiveInDays,
                                        int consecutiveOutDays,
                                        double cumulativeInMvPct,
                                        double cumulativeOutMvPct) {
        Map<String, Object> m = thresholdMap();
        m.put("consecutiveInDays", consecutiveInDays);
        m.put("consecutiveOutDays", consecutiveOutDays);
        m.put("cumulativeInMvPct", cumulativeInMvPct);
        m.put("cumulativeOutMvPct", cumulativeOutMvPct);
        int need = SentimentMdThresholds.G4_STREAK_DAYS;
        double pct = SentimentMdThresholds.G4_CUMULATIVE_MV_PCT;
        if (consecutiveInDays >= need && cumulativeInMvPct > pct) {
            return FactorResult.builder("G4_STOCK_NORTH_STREAK")
                    .level(FactorSignalLevel.BULLISH)
                    .summary("北向规律吸筹：连续净流入≥" + need + " 日且累计>" + pct + "% 流通市值")
                    .metrics(m)
                    .build();
        }
        if (consecutiveOutDays >= need && cumulativeOutMvPct > pct) {
            return FactorResult.builder("G4_STOCK_NORTH_STREAK")
                    .level(FactorSignalLevel.BEARISH)
                    .summary("北向坚决离场：连续净流出≥" + need + " 日且累计>" + pct + "% 流通市值，不宜抄底")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("G4_STOCK_NORTH_STREAK")
                .level(FactorSignalLevel.NEUTRAL)
                .summary("北向流入/流出纪律性未达文档极端档")
                .metrics(m)
                .build();
    }
}
