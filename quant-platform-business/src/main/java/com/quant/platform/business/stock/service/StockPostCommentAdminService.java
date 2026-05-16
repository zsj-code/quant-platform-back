package com.quant.platform.business.stock.service;


import com.quant.platform.business.stock.entity.StockPostCommentEntity;
import com.quant.platform.common.api.PageResult;

import java.util.List;

public interface StockPostCommentAdminService {

    StockPostCommentEntity getById(String id);

    /**
     * 按帖子 external_id 查询评论（按 publish_time 升序，便于楼层回放）。
     */
    List<StockPostCommentEntity> listByPostExternalId(String postExternalId);

    PageResult<StockPostCommentEntity> pageByPostExternalId(String postExternalId, Long current, Long size);

    void upsert(StockPostCommentEntity entity);

    void upsertBatch(List<StockPostCommentEntity> entities);
}

