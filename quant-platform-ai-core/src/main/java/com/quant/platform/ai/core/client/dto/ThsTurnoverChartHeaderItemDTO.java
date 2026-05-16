package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 同花顺市场成交额分时图 {@code header} 单项（汇总指标）。
 */
public record ThsTurnoverChartHeaderItemDTO(
        /** 指标数值（多为成交额累计，单位与接口一致） */
        @JsonProperty("val") Long val,
        /** 中文名称 */
        @JsonProperty("name") String name,
        /** 字段键，如 {@code turnover}、{@code turnover_pre} */
        @JsonProperty("key") String key) {
}
