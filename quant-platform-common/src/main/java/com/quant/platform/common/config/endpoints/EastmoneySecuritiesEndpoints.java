package com.quant.platform.common.config.endpoints;

/**
 * 东方财富 {@code datacenter.eastmoney.com/securities}（F10 财务等）。
 *
 * @see <a href="https://datacenter.eastmoney.com/securities/api/data/get">securities/api/data/get</a>
 */
public final class EastmoneySecuritiesEndpoints {
    private EastmoneySecuritiesEndpoints() {
    }

    public static final String BASE_DATACENTER_SECURITIES = "https://datacenter.eastmoney.com";

    public static final String SECURITIES_DATA_GET_PATH = "/securities/api/data/get";

    /** F10 利润表（一般企业） */
    public static final String TYPE_RPT_F10_FINANCE_GINCOME = "RPT_F10_FINANCE_GINCOME";

    public static final String STY_APP_F10_GINCOME = "APP_F10_GINCOME";

    /** F10 现金流量表（一般企业） */
    public static final String TYPE_RPT_F10_FINANCE_GCASHFLOW = "RPT_F10_FINANCE_GCASHFLOW";

    public static final String STY_APP_F10_GCASHFLOW = "APP_F10_GCASHFLOW";

    /** F10 资产负债表（一般企业） */
    public static final String TYPE_RPT_F10_FINANCE_GBALANCE = "RPT_F10_FINANCE_GBALANCE";

    public static final String STY_F10_FINANCE_GBALANCE = "F10_FINANCE_GBALANCE";

    public static final String SOURCE_HSF10 = "HSF10";

    public static final String CLIENT_PC = "PC";
}
