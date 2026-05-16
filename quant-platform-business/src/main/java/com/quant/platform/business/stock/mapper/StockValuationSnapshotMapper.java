package com.quant.platform.business.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.stock.entity.StockValuationSnapshotEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StockValuationSnapshotMapper extends BaseMapper<StockValuationSnapshotEntity> {

    /**
     * 批量 upsert：依赖表上 symbol 唯一键（uk_stock_valuation_symbol）。
     */
    void upsertBatch(@Param("list") List<StockValuationSnapshotEntity> list);
}
