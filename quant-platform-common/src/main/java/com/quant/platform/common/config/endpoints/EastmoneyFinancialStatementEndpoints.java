package com.quant.platform.common.config.endpoints;

/**
 * 东方财富数据中心 {@code datacenter-web.eastmoney.com}（财务报表 /
 * {@code financial_statement} 同步）。
 *
 * @see <a href=
 *      "https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>
 */
public final class EastmoneyFinancialStatementEndpoints {
    private EastmoneyFinancialStatementEndpoints() {
    }

    public static final String BASE_DATACENTER_WEB = "https://datacenter-web.eastmoney.com";

    public static final String DATA_V1_GET_PATH = "/api/data/v1/get";

    /** 上市公司质押比例等（数据中心列表，如股权质押） */
    public static final String REPORT_RPT_CSDC_LIST = "RPT_CSDC_LIST";

    /** 个股融资融券明细（按日，字段含融资余额、融券余量等，见东财 RPTA_WEB_RZRQ_GGMX） */
    public static final String REPORT_RPTA_WEB_RZRQ_GGMX = "RPTA_WEB_RZRQ_GGMX";

    /** 全市场融资融券历史汇总（按日，见东财 RPTA_RZRQ_LSHJ） */
    public static final String REPORT_RPTA_RZRQ_LSHJ = "RPTA_RZRQ_LSHJ";

    /** 列表页默认条数（与站点常见分页一致） */
    public static final int DEFAULT_PAGE_SIZE = 20;
}
