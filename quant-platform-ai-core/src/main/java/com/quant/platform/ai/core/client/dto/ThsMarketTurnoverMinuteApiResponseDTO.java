package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 同花顺 dq「扶摇」{@code get_chart_data} 完整 JSON 响应（含 {@code status_code}）。
 */
public record ThsMarketTurnoverMinuteApiResponseDTO(
        @JsonProperty("status_code") int statusCode,
        @JsonProperty("status_msg") String statusMsg,
        @JsonProperty("data") ThsMarketTurnoverMinuteDataDTO data) {
}
