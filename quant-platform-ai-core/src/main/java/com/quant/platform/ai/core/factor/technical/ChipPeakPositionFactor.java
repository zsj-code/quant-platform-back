package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import java.util.List;

/**
 * 筹码峰位置：
 * - 需要识别当前价格上下最近的两个最大筹码峰（依赖筹码分布/分价成交数据）。
 *
 * 当前项目只有日线 OHLCV，无法构建筹码峰，因此先留空实现。
 */
public class ChipPeakPositionFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "chip.peak_position";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.CHIP_DISTRIBUTION;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        // TODO: 需要筹码分布（分价成交）数据来识别筹码峰。
        return FactorResult.unavailable(factorKey(), "缺少筹码分布/分价成交数据：无法识别筹码峰位置");
    }
}

