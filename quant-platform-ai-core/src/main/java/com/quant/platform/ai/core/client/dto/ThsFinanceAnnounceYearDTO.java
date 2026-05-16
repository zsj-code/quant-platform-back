package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * 同花顺 {@code finance/announce/detail} 中按自然年聚合的一档数据。
 * <p>
 * 接口 JSON 字段名为 {@code opinion}，语义为<strong>审核意见</strong>（与页面「年报」审计结论一致；当年仅有一季报等时可能为空串）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ThsFinanceAnnounceYearDTO(
        String year,
        /** 审核意见（对应上游 JSON 的 {@code opinion}） */
        @JsonProperty("opinion") String auditOpinion,
        @JsonProperty("report_list") List<ThsFinanceReportItemDTO> reportList) {

    public ThsFinanceAnnounceYearDTO {
        reportList = reportList == null ? List.of() : List.copyOf(reportList);
    }

    /**
     * 该自然年下的「年报」报告项（{@code period == "年报"}），若不存在则 empty。
     */
    public Optional<ThsFinanceReportItemDTO> annualReport() {
        return reportList.stream().filter(r -> "年报".equals(r.period())).findFirst();
    }
}
