package com.quant.platform.business.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.stock.entity.StockIndustryValuationEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StockIndustryValuationMapper extends BaseMapper<StockIndustryValuationEntity> {

    /**
     * 批量 upsert：依赖表上 symbol 唯一键（uk_stock_industry_valuation_symbol）。
     */
    void upsertBatch(@Param("list") List<StockIndustryValuationEntity> list);
}
