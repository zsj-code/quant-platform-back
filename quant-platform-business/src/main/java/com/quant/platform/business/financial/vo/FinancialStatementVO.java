package com.quant.platform.business.financial.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStatementVO {
    private String id;
    private String code;
    private String symbol;
    private String reportType;
    private String reportTypeDesc;
    private LocalDate reportDate;
    private String rawJson;
    private String sourceReportName;
    private LocalDateTime fetchedAt;
}
