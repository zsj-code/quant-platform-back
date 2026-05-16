package com.quant.platform.business.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.stock.entity.StockPostCommentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StockPostCommentMapper extends BaseMapper<StockPostCommentEntity> {

    /**
     * 批量 upsert：依赖唯一键 (source, external_id)。
     */
    void upsertBatch(@Param("list") List<StockPostCommentEntity> list);
}

