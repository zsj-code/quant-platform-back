package com.quant.platform.business.stock.convert;

import com.quant.platform.business.stock.entity.StockAnnouncementEntity;
import com.quant.platform.business.stock.vo.StockAnnouncementVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface StockAnnouncementAdminConvert {
    StockAnnouncementAdminConvert INSTANCE = Mappers.getMapper(StockAnnouncementAdminConvert.class);

    StockAnnouncementVO toVO(StockAnnouncementEntity entity);

    List<StockAnnouncementVO> toVOList(List<StockAnnouncementEntity> entities);
}
