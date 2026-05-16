package com.quant.platform.common.config.endpoints;

/**
 * 东方财富研报 API（reportapi.eastmoney.com）。
 * <p>
 * 个股研报一般使用 POST {@code /report/list2}；行业研报等可使用 GET {@code /report/list}。
 */
public final class EastmoneyResearchReportEndpoints {

    private EastmoneyResearchReportEndpoints() {
    }

    public static final String BASE_REPORT_API = "https://reportapi.eastmoney.com";
    /** 行业研报等列表 */
    public static final String PATH_LIST = "/report/list";
    /** 个股研报列表（JSON Body） */
    public static final String PATH_LIST2 = "/report/list2";
}
