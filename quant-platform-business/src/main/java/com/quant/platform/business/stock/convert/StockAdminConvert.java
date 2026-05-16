package com.quant.platform.business.stock.convert;

import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.vo.StockVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface StockAdminConvert {
    StockAdminConvert INSTANCE = Mappers.getMapper(StockAdminConvert.class);

    StockVO toVO(StockEntity entity);

    List<StockVO> toVOList(List<StockEntity> entities);
}
