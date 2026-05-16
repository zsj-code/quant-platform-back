package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 东财 {@code RPTA_WEB_RZRQ_GGMX} 单条个股融资融券明细（与接口 {@code data} 中单行 JSON 字段对应）。
 */
public record EastmoneyMarginTradingStockRowDTO(
        /** 交易日期（接口原样，常含 {@code 00:00:00}） */
        @JsonProperty("DATE") String date,
        /** 市场板块说明，如 {@code 融资融券_深证} */
        @JsonProperty("MARKET") String market,
        /** 证券 6 位代码 */
        @JsonProperty("SCODE") String scode,
        /** 证券简称 */
        @JsonProperty("SECNAME") String secName,
        /** 融资余额（元） */
        @JsonProperty("RZYE") Long rzye,
        /** 融券余量（股/份） */
        @JsonProperty("RQYL") Long rqyl,
        /** 融资融券余额合计（元） */
        @JsonProperty("RZRQYE") Long rzrqye,
        /** 融券余额（元） */
        @JsonProperty("RQYE") Long rqye,
        /** 融券卖出量 */
        @JsonProperty("RQMCL") Long rqmcl,
        /** 融资融券余额差值（元） */
        @JsonProperty("RZRQYECZ") Long rzrqyecz,
        /** 融资买入额（元） */
        @JsonProperty("RZMRE") Long rzmre,
        /** 总市值（元） */
        @JsonProperty("SZ") BigDecimal sz,
        /** 融资余额占流通市值比（%） */
        @JsonProperty("RZYEZB") BigDecimal rzyezb,
        /** 近 3 日融资买入额（元） */
        @JsonProperty("RZMRE3D") Long rzmre3d,
        /** 近 5 日融资买入额（元） */
        @JsonProperty("RZMRE5D") Long rzmre5d,
        /** 近 10 日融资买入额（元） */
        @JsonProperty("RZMRE10D") Long rzmre10d,
        /** 融资偿还额（元） */
        @JsonProperty("RZCHE") Long rzche,
        /** 近 3 日融资偿还额（元） */
        @JsonProperty("RZCHE3D") Long rzche3d,
        /** 近 5 日融资偿还额（元） */
        @JsonProperty("RZCHE5D") Long rzche5d,
        /** 近 10 日融资偿还额（元） */
        @JsonProperty("RZCHE10D") Long rzche10d,
        /** 融资净买入额（元），可为负 */
        @JsonProperty("RZJME") Long rzjm,
        /** 近 3 日融资净买入额（元） */
        @JsonProperty("RZJME3D") Long rzjm3d,
        /** 近 5 日融资净买入额（元） */
        @JsonProperty("RZJME5D") Long rzjm5d,
        /** 近 10 日融资净买入额（元） */
        @JsonProperty("RZJME10D") Long rzjm10d,
        /** 近 3 日融券卖出量 */
        @JsonProperty("RQMCL3D") Long rqmcl3d,
        /** 近 5 日融券卖出量 */
        @JsonProperty("RQMCL5D") Long rqmcl5d,
        /** 近 10 日融券卖出量 */
        @JsonProperty("RQMCL10D") Long rqmcl10d,
        /** 融券偿还量 */
        @JsonProperty("RQCHL") Long rqchl,
        /** 近 3 日融券偿还量 */
        @JsonProperty("RQCHL3D") Long rqchl3d,
        /** 近 5 日融券偿还量 */
        @JsonProperty("RQCHL5D") Long rqchl5d,
        /** 近 10 日融券偿还量 */
        @JsonProperty("RQCHL10D") Long rqchl10d,
        /** 融券净卖出量（可为负表示净偿还） */
        @JsonProperty("RQJMG") Long rqjmg,
        /** 近 3 日融券净卖出量 */
        @JsonProperty("RQJMG3D") Long rqjmg3d,
        /** 近 5 日融券净卖出量 */
        @JsonProperty("RQJMG5D") Long rqjmg5d,
        /** 近 10 日融券净卖出量 */
        @JsonProperty("RQJMG10D") Long rqjmg10d,
        /** 收盘价 */
        @JsonProperty("SPJ") BigDecimal spj,
        /** 涨跌幅（%） */
        @JsonProperty("ZDF") BigDecimal zdf,
        /** 融资余额 3 日变动率（%） */
        @JsonProperty("RCHANGE3DCP") BigDecimal rchange3dcp,
        /** 融资余额 5 日变动率（%） */
        @JsonProperty("RCHANGE5DCP") BigDecimal rchange5dcp,
        /** 融资余额 10 日变动率（%） */
        @JsonProperty("RCHANGE10DCP") BigDecimal rchange10dcp,
        /** 科创板等标识（接口数值，含义以东财为准） */
        @JsonProperty("KCB") Integer kcb,
        /** 交易市场代码 */
        @JsonProperty("TRADE_MARKET_CODE") String tradeMarketCode,
        /** 交易市场名称，如 {@code 深交所主板} */
        @JsonProperty("TRADE_MARKET") String tradeMarket,
        /** 融资余额环比（比例，接口字段 {@code FIN_BALANCE_GR}） */
        @JsonProperty("FIN_BALANCE_GR") BigDecimal finBalanceGr,
        /** 带市场后缀代码，如 {@code 002456.SZ} */
        @JsonProperty("SECUCODE") String secucode) {
}
