package com.quant.platform.common.enums;

/**
 * 财务报表 {@code reportName}（数据中心接口，与表 {@code financial_statement} 的报表维度对应）。
 */
public enum EastmoneyFinancialStatementReportTypeEnum {
    /** 利润表 */
    INCOME("RPT_DMSK_FN_INCOME"),
    /** 资产负债表 */
    BALANCE("RPT_DMSK_FN_BALANCE"),
    /** 现金流量表 */
    CASHFLOW("RPT_DMSK_FN_CASHFLOW");

    private final String reportName;

    EastmoneyFinancialStatementReportTypeEnum(String reportName) {
        this.reportName = reportName;
    }

    public String getReportName() {
        return reportName;
    }

}
