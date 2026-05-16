package com.quant.platform.business.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.platform.ai.core.port.FinancialStatementPort;
import com.quant.platform.business.financial.entity.FinancialStatementEntity;
import com.quant.platform.business.financial.mapper.FinancialStatementMapper;
import com.quant.platform.common.dto.FinancialStatementDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinancialStatementPortAdapter implements FinancialStatementPort {
    private final FinancialStatementMapper financialStatementMapper;

    public FinancialStatementPortAdapter(FinancialStatementMapper financialStatementMapper) {
        this.financialStatementMapper = financialStatementMapper;
    }

    @Override
    public List<FinancialStatementDTO> listStatementsDesc(String code,
                                                         String reportTypeEnumName,
                                                         LocalDate reportDateBeginInclusive,
                                                         LocalDate reportDateEndInclusive) {
        if (code == null || code.isBlank() || reportTypeEnumName == null || reportTypeEnumName.isBlank()) {
            return List.of();
        }
        List<FinancialStatementEntity> rows = financialStatementMapper.selectList(new LambdaQueryWrapper<FinancialStatementEntity>()
                .eq(FinancialStatementEntity::getCode, code)
                .eq(FinancialStatementEntity::getReportType, reportTypeEnumName)
                .between(reportDateBeginInclusive != null && reportDateEndInclusive != null,
                        FinancialStatementEntity::getReportDate, reportDateBeginInclusive, reportDateEndInclusive)
                .ge(reportDateBeginInclusive != null && reportDateEndInclusive == null,
                        FinancialStatementEntity::getReportDate, reportDateBeginInclusive)
                .le(reportDateBeginInclusive == null && reportDateEndInclusive != null,
                        FinancialStatementEntity::getReportDate, reportDateEndInclusive)
                .orderByDesc(FinancialStatementEntity::getReportDate)
                .orderByDesc(FinancialStatementEntity::getFetchedAt));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<FinancialStatementDTO> out = new ArrayList<>(rows.size());
        for (FinancialStatementEntity e : rows) {
            if (e != null) {
                out.add(toDto(e));
            }
        }
        return out;
    }

    private static FinancialStatementDTO toDto(FinancialStatementEntity e) {
        FinancialStatementDTO dto = new FinancialStatementDTO();
        dto.setCode(e.getCode());
        dto.setSymbol(e.getSymbol());
        dto.setReportType(e.getReportType());
        dto.setReportDate(e.getReportDate());
        dto.setRawJson(e.getRawJson());
        dto.setSourceReportName(e.getSourceReportName());
        dto.setFetchedAt(e.getFetchedAt());
        return dto;
    }
}

