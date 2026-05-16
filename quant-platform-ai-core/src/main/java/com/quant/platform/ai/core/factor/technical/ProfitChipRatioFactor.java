package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import java.util.List;

/**
 * 获利盘比例：
 * - 以当日收盘价为基准，统计历史成交中处于获利状态的比例。
 *
 * 需要：
 * - 历史成交的成本分布（筹码分布）或逐笔成交成本数据。
 *
 * 当前项目只有日线 OHLCV，无法精确计算，因此先留空实现。
 */
public class ProfitChipRatioFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "chip.profit_ratio";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.CHIP_DISTRIBUTION;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        // TODO: 需要筹码成本分布/逐笔成交成本数据才能计算获利盘比例。
        return FactorResult.unavailable(factorKey(), "缺少筹码成本分布数据：无法计算获利盘比例");
    }
}

