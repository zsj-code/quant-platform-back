package com.quant.platform.business.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.stock.entity.StockEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StockMapper extends BaseMapper<StockEntity> {

    void insertBatch(@Param("list") List<StockEntity> list);
}
