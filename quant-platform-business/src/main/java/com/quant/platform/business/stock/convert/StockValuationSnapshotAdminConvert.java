package com.quant.platform.business.stock.convert;

import com.quant.platform.business.stock.dto.StockValuationSnapshotDTO;
import com.quant.platform.business.stock.entity.StockValuationSnapshotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface StockValuationSnapshotAdminConvert {
    StockValuationSnapshotAdminConvert INSTANCE = Mappers.getMapper(StockValuationSnapshotAdminConvert.class);

    StockValuationSnapshotDTO toDTO(StockValuationSnapshotEntity entity);

    List<StockValuationSnapshotDTO> toDTOList(List<StockValuationSnapshotEntity> entities);
}
