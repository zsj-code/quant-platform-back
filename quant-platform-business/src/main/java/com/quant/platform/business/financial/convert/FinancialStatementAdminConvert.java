package com.quant.platform.business.financial.convert;

import com.quant.platform.business.financial.entity.FinancialStatementEntity;
import com.quant.platform.business.financial.vo.FinancialStatementVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface FinancialStatementAdminConvert {
    FinancialStatementAdminConvert INSTANCE = Mappers.getMapper(FinancialStatementAdminConvert.class);

    @Mapping(target = "reportTypeDesc", expression = "java(com.quant.platform.common.enums.FinancialReportTypeEnum.fromCode(entity.getReportType()) == null ? null : com.quant.platform.common.enums.FinancialReportTypeEnum.fromCode(entity.getReportType()).getDesc())")
    FinancialStatementVO toVO(FinancialStatementEntity entity);

    List<FinancialStatementVO> toVOList(List<FinancialStatementEntity> entities);
}
