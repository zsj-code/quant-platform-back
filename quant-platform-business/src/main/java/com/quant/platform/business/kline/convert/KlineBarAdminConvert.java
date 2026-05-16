package com.quant.platform.business.kline.convert;

import com.quant.platform.business.kline.entity.KlineBarEntity;
import com.quant.platform.business.kline.vo.KlineBarVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface KlineBarAdminConvert {
    KlineBarAdminConvert INSTANCE = Mappers.getMapper(KlineBarAdminConvert.class);

    KlineBarVO toVO(KlineBarEntity entity);

    List<KlineBarVO> toVOList(List<KlineBarEntity> entities);
}
