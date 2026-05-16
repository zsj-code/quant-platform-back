package com.quant.platform.ai.core.factor.technical;


import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import com.quant.platform.common.dto.KlineBarDTO;

import java.util.List;

public interface TechnicalFactor {
    String factorKey();

    default TechnicalFactorGroup group() {
        return TechnicalFactorGroup.OTHER;
    }

    /**
     * 当前因子希望使用的K线周期：
     * - 大部分技术指标按日线（D）实现
     * - 少数需要分钟线（M1），例如开盘/尾盘30分钟成交量占比
     */
    default String requiredIntervalType() {
        return KlineIntervalTypeEnum.D.getCode();
    }

    /**
     * bars 必须按时间升序（最老 -> 最新）。
     */
    FactorResult evaluate(List<KlineBarDTO> bars);
}

