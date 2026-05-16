package com.quant.platform.business.financial.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStatementQueryDTO {
    private String code;
    private String reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long current;
    private Long size;
}
