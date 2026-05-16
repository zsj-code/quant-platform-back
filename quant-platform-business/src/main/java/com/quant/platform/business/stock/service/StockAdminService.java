package com.quant.platform.business.stock.service;


import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.common.api.PageResult;

import java.util.List;
import java.util.Set;

public interface StockAdminService {
    StockEntity getById(String id);

    PageResult<StockEntity> page(String keyword, Long current, Long size);

    List<StockEntity> list();

    /** 未退市股票（is_delisted 为 null 或 LISTED）。 */
    List<StockEntity> listNonDelisted();

    boolean existByCode(String code);

    void addStock(StockEntity stockEntity);

    void addStockBatch(List<StockEntity> stockEntityList);

    Set<String> queryByStockCodeList(List<String> stockCodeList);
}
