package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 同花顺 {@code finance/announce/detail} 中单条报告摘要。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ThsFinanceReportItemDTO(
        String report,
        @JsonProperty("report_name") String reportName,
        String date,
        String seq,
        @JsonProperty("mobile_url") String mobileUrl,
        @JsonProperty("client_url") String clientUrl,
        String period) {
}
