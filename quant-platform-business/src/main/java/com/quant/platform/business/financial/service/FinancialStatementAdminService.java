package com.quant.platform.business.financial.service;


import com.quant.platform.business.financial.entity.FinancialStatementEntity;
import com.quant.platform.common.api.PageResult;

import java.time.LocalDate;
import java.util.List;

public interface FinancialStatementAdminService {
    FinancialStatementEntity getById(String id);

    PageResult<FinancialStatementEntity> page(String code, String reportType, LocalDate startDate, LocalDate endDate,
                                              Long current, Long size);

    List<FinancialStatementEntity> list(String code, String reportType, LocalDate startDate, LocalDate endDate);

    /**
     * 某只股票下去重后的报告期（按报告日倒序）。
     */
    List<LocalDate> listDistinctReportDatesByCode(String code);

    /**
     * 时间窗内报表维度投影：仅 {@code report_type}、{@code report_date}，不加载 {@code raw_json}，供 Agent 做三张表齐备度等轻量统计。
     */
    List<FinancialStatementEntity> listReportDimensionsInWindow(String code, LocalDate startInclusive, LocalDate endInclusive);

    /**
     * 按 code + report_type + report_date 幂等写入：已存在则更新，否则插入。
     */
    void upsert(FinancialStatementEntity entity);

    /**
     * 批量幂等写入，语义与 {@link #upsert} 相同；{@code null} 元素跳过。
     */
    void upsertBatch(List<FinancialStatementEntity> entities);
}
