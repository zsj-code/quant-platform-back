package com.quant.platform.common.config.endpoints;

/**
 * 东方财富相关 URL/参数统一管理。
 */
public final class EastmoneyEndpoints {
    private EastmoneyEndpoints() {
    }

    public static final String BASE_PUSH2 = "https://push2.eastmoney.com";

    /** K 线历史域名（与列表/快照 push2 分离） */
    public static final String BASE_PUSH2HIS = "https://push2his.eastmoney.com";

    /** 个股快照/指标域名（与 K 线 push2his 分离） */
    public static final String BASE_PUSH2_SNAPSHOT = BASE_PUSH2;

    /**
     * clist/get 常用鉴权参数。 缺失时常见表现：rc=102, data=null。
     */
    public static final String QT_UT = "bd1d9ddb04089700cf9c27f6f7426281";

    /**
     * A股列表（简化版，仅沪深主板/创业板） fs: - 深A: m:0+t:6 - 创业板: m:0+t:80 - 沪A: m:1+t:2 - 科创板:
     * m:1+t:23
     */
    public static final String STOCKS_CLIST_PATH = "/api/qt/clist/get";

    public static final String STOCKS_FS = "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23";

    /** clist/get 返回字段：f12=股票编码，f14=股票名称（stock/get 同编号含义不同，见 {@link #STOCK_VALUATION_FIELDS}） */
    public static final String STOCKS_FIELDS = "f12,f14";

    /**
     * A 股列表 fs 参数。
     * <p>
     * 注意：URI 构建/解析时，queryParam 值中包含空格会触发非法字符异常。 这里统一使用无空格写法（用 '+' 连接），与东财接口兼容。
     * 示例：fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23
     */
    public static final String A_STOCKS_FS = "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23";

    /** 个股快照（市盈率/市净率/市值等） */
    public static final String STOCK_GET_PATH = "/api/qt/stock/get";

    /** K 线：push2his + kline/get */
    public static final String STOCK_KLINE_GET_PATH = "/api/qt/stock/kline/get";

    /** K线默认条数限制 */
    public static final int STOCK_KLINE_LMT_DEFAULT = 2000;

    /** stock/get fields1 常用字段 */
    public static final String STOCK_GET_FIELDS1 = "f1,f2,f3,f4,f5";

    /**
     * kline/get 的 fields2（单日 K 线串）：f51=日期, f52=开盘, f53=收盘, f54=最高, f55=最低, f56=成交量, f57=成交额,
     * f58=振幅%, f59=涨跌幅%, f60=涨跌额, f61=换手率%（与 stock/get 同编号 fxx 含义不同）。
     */
    public static final String STOCK_GET_FIELDS2_KLINE =
        "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";

    /**
     * stock/get 快照字段（单一 {@code fields} 查询参数）。
     * <p>
     * 东财该接口对 {@code fields1}+{@code fields2} 拆分时，f162、f163 等估值字段常不返回；合并为 {@code fields} 后可正常取数。
     * 证券代码/名称 f57、f58。
     */
    public static final String STOCK_VALUATION_FIELDS =
        "f57,f58,f43,f44,f45,f46,f47,f48,f50,f51,f52,f60,f71,"
            + "f84,f85,f116,f117,f162,f163,f164,f165,f167,f168,f171";

    /** 常用默认参数 */
    public static final String QT_FLTT = "2";
    public static final String QT_INVT = "2";
    public static final String QT_PO = "1";
    public static final String QT_NP = "1";
}
