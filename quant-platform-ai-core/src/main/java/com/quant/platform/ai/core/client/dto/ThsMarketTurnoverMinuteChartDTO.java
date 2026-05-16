package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 同花顺 {@code chart_key=turnover_minute} 返回体中的 {@code data.charts} 对象（全市场成交额分时）。
 * <p>
 * {@code point_list} 为二维数组，每行与 {@link #pointKeyList} 顺序一致，通常为：
 * {@code [timestamp, turnover, turnover_pre, turnover_change]}。
 */
public record ThsMarketTurnoverMinuteChartDTO(
        /** 分时点数（与 {@code x_label_list} 等长时的语义以同花顺前端为准） */
        @JsonProperty("total") int total,
        /** 图表名称，如「市场成交额分时」 */
        @JsonProperty("name") String name,
        /** 顶部汇总指标（当日成交额、昨日成交额、较昨日变动、预测全天成交额等） */
        @JsonProperty("header") List<ThsTurnoverChartHeaderItemDTO> header,
        /** 分时点序列；每元素为一行数值，顺序同 {@link #pointKeyList} */
        @JsonProperty("point_list") List<List<Long>> pointList,
        /** 数据更新时间 */
        @JsonProperty("mtime") String mtime,
        /** 各序列说明 */
        @JsonProperty("lines") List<ThsChartLineMetaDTO> lines,
        /** 每列字段键，与 {@code point_list} 列一一对应 */
        @JsonProperty("point_key_list") List<String> pointKeyList,
        /** 图表键，如 {@code turnover_minute} */
        @JsonProperty("key") String key,
        /** 横轴时间标签 */
        @JsonProperty("x_label_list") List<String> xLabelList) {
}
