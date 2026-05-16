package com.quant.platform.ai.core.factor.technical;

import java.util.List;
import java.util.Map;

/**
 * 统一汇总技术因子，并按分组归类。
 * 后续如果引入 Spring 扫描/注册机制，可替换为自动发现。
 */
public final class TechnicalFactorCatalog {
    private TechnicalFactorCatalog() {
    }

    public static Map<TechnicalFactorGroup, List<TechnicalFactor>> allGrouped() {
        return Map.of(
                TechnicalFactorGroup.TREND_STRUCTURE, List.of(
                        new MultiPeriodMovingAverageAlignmentFactor(),
                        new Ma60AnnualizedSlopeFactor(),
                        new PriceMa200DistanceFactor(),
                        new AdxTrendStrengthFactor()
                ),
                TechnicalFactorGroup.VOLUME_FLOW, List.of(
                        new RelativeVolumeFactor(),
                        new VolumePriceDivergenceFactor(),
                        new OpeningClosingVolumeAnomalyFactor(),
                        new TurnoverRateAlertFactor()
                ),
                TechnicalFactorGroup.PATTERN_DIVERGENCE, List.of(
                        new MacdDivergenceFactor(),
                        new RsiExtremeFactor(),
                        new BreakoutValidationFactor(),
                        new IntradayReversalFactor()
                ),
                TechnicalFactorGroup.CHIP_DISTRIBUTION, List.of(
                        new ChipConcentrationFactor(),
                        new ProfitChipRatioFactor(),
                        new ChipPeakPositionFactor()
                )
        );
    }
}

