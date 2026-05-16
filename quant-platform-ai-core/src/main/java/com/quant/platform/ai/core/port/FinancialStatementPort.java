package com.quant.platform.ai.core.port;

import com.quant.platform.common.dto.FinancialStatementDTO;

import java.time.LocalDate;
import java.util.List;

public interface FinancialStatementPort {

    /**
     * 按 report_date desc, fetched_at desc 返回（与现有业务查询保持一致）。
     */
    List<FinancialStatementDTO> listStatementsDesc(String code,
                                                   String reportTypeEnumName,
                                                   LocalDate reportDateBeginInclusive,
                                                   LocalDate reportDateEndInclusive);
}

