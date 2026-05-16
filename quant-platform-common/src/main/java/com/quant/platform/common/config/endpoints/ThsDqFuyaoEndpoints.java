package com.quant.platform.common.config.endpoints;

/**
 * 同花顺 dq 域「扶摇」市场分析图表 API（浏览器端行情/市场页使用的 JSON）。
 *
 * @see <a href="https://dq.10jqka.com.cn/fuyao/market_analysis_api/chart/v1/get_chart_data">get_chart_data</a>
 */
public final class ThsDqFuyaoEndpoints {

    private ThsDqFuyaoEndpoints() {
    }

    public static final String BASE_DQ_10JQKA = "https://dq.10jqka.com.cn";

    /** 图表数据：{@code ?chart_key=} */
    public static final String MARKET_ANALYSIS_CHART_GET_PATH = "/fuyao/market_analysis_api/chart/v1/get_chart_data";

    /** 全市场成交额分时 */
    public static final String CHART_KEY_TURNOVER_MINUTE = "turnover_minute";
}
