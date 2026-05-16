package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 同花顺 {@code /basicapi/finance/announce/detail/} 原始响应体。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ThsFinanceAnnounceDetailResponseDTO(
        @JsonProperty("status_code") int statusCode,
        @JsonProperty("status_msg") String statusMsg,
        List<ThsFinanceAnnounceYearDTO> data) {

    public ThsFinanceAnnounceDetailResponseDTO {
        data = data == null ? List.of() : List.copyOf(data);
    }
}
