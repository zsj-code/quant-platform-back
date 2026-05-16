package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 同花顺 {@code get_chart_data} 响应中的 {@code data} 节点。
 */
public record ThsMarketTurnoverMinuteDataDTO(
        @JsonProperty("charts") ThsMarketTurnoverMinuteChartDTO charts) {
}
