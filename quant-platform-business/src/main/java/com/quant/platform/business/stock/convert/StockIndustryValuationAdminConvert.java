package com.quant.platform.business.stock.convert;

import com.quant.platform.business.stock.dto.StockIndustryValuationDTO;
import com.quant.platform.business.stock.entity.StockIndustryValuationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface StockIndustryValuationAdminConvert {
    StockIndustryValuationAdminConvert INSTANCE = Mappers.getMapper(StockIndustryValuationAdminConvert.class);

    StockIndustryValuationDTO toDTO(StockIndustryValuationEntity entity);

    List<StockIndustryValuationDTO> toDTOList(List<StockIndustryValuationEntity> entities);
}
