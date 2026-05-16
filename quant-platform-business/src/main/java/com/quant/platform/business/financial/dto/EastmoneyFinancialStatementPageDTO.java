package com.quant.platform.business.financial.dto;

import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * 东方财富数据中心财务报表分页结果（对应库表 {@code financial_statement} 的数据来源之一；行数据为动态列名，见接口
 * {@code columns=ALL}）。
 */
@Value
public class EastmoneyFinancialStatementPageDTO {
    String reportName;
    String securityCode;
    int pageNumber;
    int pageSize;
    long total;
    /** 总页数；接口未返回时可能为 null */
    Integer totalPages;
    List<Map<String, Object>> rows;
}
