package com.quant.platform.business.stock.service;


import com.quant.platform.business.stock.entity.StockPostEntity;
import com.quant.platform.common.api.PageResult;

import java.util.List;

public interface StockPostAdminService {

    StockPostEntity getById(String id);

    /**
     * 按标的查询帖子列表（按 publish_time 倒序）。
     */
    List<StockPostEntity> listBySecCode(String secCode);

    PageResult<StockPostEntity> pageBySecCode(String secCode, Long current, Long size);

    /**
     * 写入或更新：按 (source, external_id) 幂等。
     */
    void upsert(StockPostEntity entity);

    /**
     * 批量幂等写入：按 (source, external_id) 去重后执行单次 INSERT…ON DUPLICATE KEY UPDATE；null 元素跳过。
     */
    void upsertBatch(List<StockPostEntity> entities);
}

