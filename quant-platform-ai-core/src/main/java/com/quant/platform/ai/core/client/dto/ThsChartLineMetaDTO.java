package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 同花顺图表 {@code lines} 中序列元数据（名称与字段键）。
 */
public record ThsChartLineMetaDTO(
        @JsonProperty("name") String name,
        @JsonProperty("key") String key) {
}
