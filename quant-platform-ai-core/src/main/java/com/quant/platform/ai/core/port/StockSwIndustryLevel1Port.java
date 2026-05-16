package com.quant.platform.ai.core.port;

import java.util.Optional;

/**
 * 个股申万一级行业名称（用于基本面因子按行业阈值等）。
 */
public interface StockSwIndustryLevel1Port {

    /**
     * @param secCode 6 位证券代码
     * @return 行业名称（与申万一级标准名一致时，因子侧行业阈值表可命中）；无数据则 empty
     */
    Optional<String> findSwIndustryLevel1BySecCode(String secCode);
}
