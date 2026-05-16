package com.quant.platform.business.stock.dto;

import lombok.Value;

import java.math.BigDecimal;

/**
 * 东财 {@code /api/qt/stock/get} 个股快照（单一 {@code fields}），字段与接口 fxx 一一对应。
 * <p>
 * 注意：{@code clist/get} 等同编号 fxx 含义不同；本 DTO 仅服务于 stock/get。
 */
@Value
public class EastmoneyStockValuationDTO {
    /** 股票代码 f57 */
    String code;
    /** 股票名称 f58 */
    String name;

    /** 最新价 f43 */
    BigDecimal latestPrice;

    /** 成交量(手) f47 */
    Long volume;
    /** 成交额(元) f48 */
    BigDecimal amount;
    /** 换手率(%) f168 */
    BigDecimal turnoverRate;
    /** 量比 f50 */
    BigDecimal volumeRatio;

    /** 昨收 f60 */
    BigDecimal prevClose;
    /** 今开 f46 */
    BigDecimal openPrice;
    /** 最高 f44 */
    BigDecimal highPrice;
    /** 最低 f45 */
    BigDecimal lowPrice;
    /** 均价 f71 */
    BigDecimal avgPrice;
    /** 涨停价 f51 */
    BigDecimal limitUp;
    /** 跌停价 f52 */
    BigDecimal limitDown;
    /** 振幅(%) f171 */
    BigDecimal amplitude;

    /** 总市值(元) f116 */
    BigDecimal totalMarketCap;
    /** 流通市值(元) f117 */
    BigDecimal floatMarketCap;
    /** 总股本(股) f84 */
    BigDecimal totalShares;
    /** 流通股本(股) f85 */
    BigDecimal floatShares;

    /** 市盈率(动) f162 */
    BigDecimal peDynamic;
    /** f163（东财口径，常见为估值相关倍数） */
    BigDecimal pc;
    /** 市盈率(静)等 f164 */
    BigDecimal peStatic;
    /** 市销率 f165 */
    BigDecimal ps;
    /** 市净率 f167 */
    BigDecimal pb;

    private String rawQuoteJson;
}
