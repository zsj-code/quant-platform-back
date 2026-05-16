package com.quant.platform.business.stock.service;


import com.quant.platform.business.stock.entity.StockValuationSnapshotEntity;
import com.quant.platform.common.api.PageResult;

import java.util.List;

public interface StockValuationSnapshotAdminService {

    StockValuationSnapshotEntity getById(String id);

    StockValuationSnapshotEntity getBySymbol(String symbol);

    StockValuationSnapshotEntity getBySecCode(String secCode);

    PageResult<StockValuationSnapshotEntity> page(String secCode, String symbol, Long current, Long size);

    /**
     * 按 symbol 唯一键写入或更新（东财同步用）。
     */
    void upsert(StockValuationSnapshotEntity entity);

    /**
     * 批量幂等写入，语义与 {@link #upsert} 相同；{@code null} 元素跳过。
     */
    void upsertBatch(List<StockValuationSnapshotEntity> entities);
}
