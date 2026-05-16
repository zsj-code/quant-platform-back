package com.quant.platform.business.stock.service;


import com.quant.platform.business.stock.entity.StockAnnouncementEntity;
import com.quant.platform.common.api.PageResult;

import java.util.List;

public interface StockAnnouncementAdminService {

    StockAnnouncementEntity getById(String id);

    /**
     * 按 6 位 code 查询公告列表（按 announce_time 倒序）。
     */
    List<StockAnnouncementEntity> listByCode(String code);

    PageResult<StockAnnouncementEntity> pageByCode(String code, Long current, Long size);

    /**
     * 写入或更新；若 {@code code} 为空则从 {@code symbol} 解析填充。
     */
    void upsert(StockAnnouncementEntity entity);

    /**
     * 批量幂等写入，语义与 {@link #upsert} 相同（按 source + external_id）；{@code null} 元素跳过。
     * 实现为单次 INSERT … ON DUPLICATE KEY UPDATE，非逐条访问数据库。
     */
    void upsertBatch(List<StockAnnouncementEntity> entities);
}
