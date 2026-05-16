package com.quant.platform.business.stock.service;


import com.quant.platform.business.stock.entity.StockIndustryValuationEntity;
import com.quant.platform.common.api.PageResult;

import java.util.List;

public interface StockIndustryValuationAdminService {

    StockIndustryValuationEntity getById(String id);

    /** 按 6 位证券代码（sec_code）查询一行（每 code 通常对应唯一 symbol）。 */
    StockIndustryValuationEntity getBySecCode(String secCode);

    PageResult<StockIndustryValuationEntity> page(String secCode, String symbol, Long current, Long size);

    void upsert(StockIndustryValuationEntity entity);

    /**
     * 批量幂等写入，语义与 {@link #upsert} 相同；{@code null} 元素跳过。
     */
    void upsertBatch(List<StockIndustryValuationEntity> entities);
}
