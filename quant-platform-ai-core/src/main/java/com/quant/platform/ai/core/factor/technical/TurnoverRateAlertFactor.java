package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import java.util.List;

/**
 * 换手率警戒：当日换手率。
 *
 * 现状说明：
 * - `KlineBarEntity` 当前只有 volume，没有 free-float / 流通股本 / 成交额等字段；
 * - 换手率通常需要：成交量 / 流通股本（或成交额/流通市值），因此需要额外的“股本/流通盘”数据。
 */
public class TurnoverRateAlertFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "volume.turnover_rate_alert";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.VOLUME_FLOW;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        // TODO: 需要流通股本/自由流通市值等数据才能计算换手率。
        return FactorResult.unavailable(factorKey(), "缺少流通股本/自由流通盘数据：无法计算换手率与阈值分档");
    }
}

