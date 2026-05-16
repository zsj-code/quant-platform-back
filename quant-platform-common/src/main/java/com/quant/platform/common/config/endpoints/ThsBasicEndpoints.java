package com.quant.platform.common.config.endpoints;

/**
 * 同花顺 basic 站 API（浏览器端 finance 页使用的 JSON）。
 */
public final class ThsBasicEndpoints {

    private ThsBasicEndpoints() {
    }

    public static final String BASE_BASIC_10JQKA = "https://basic.10jqka.com.cn";

    /** 财务公告/报告期列表与历年审计意见（{@code opinion}） */
    public static final String FINANCE_ANNOUNCE_DETAIL_PATH = "/basicapi/finance/announce/detail/";

    /** 沪市 A 股（6 开头）在 basic 接口中的 {@code market} 取值 */
    public static final int MARKET_SH_A = 17;

    /** 深市 A 股在 basic 接口中的 {@code market} 取值（主板/创业板等经页面验证可用 33） */
    public static final int MARKET_SZ_A = 33;
}
