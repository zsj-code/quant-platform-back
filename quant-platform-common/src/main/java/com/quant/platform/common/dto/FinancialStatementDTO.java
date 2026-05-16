package com.quant.platform.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FinancialStatementDTO {
    private String code;
    private String symbol;
    private String reportType;
    private LocalDate reportDate;
    private String rawJson;
    private String sourceReportName;
    private LocalDateTime fetchedAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public String getSourceReportName() {
        return sourceReportName;
    }

    public void setSourceReportName(String sourceReportName) {
        this.sourceReportName = sourceReportName;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}

