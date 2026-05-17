package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 东财 {@code RPT_SHARE_HOLDER_INCREASE} 股东增减持单行，字段与接口 {@code data} 一致。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EastmoneyShareHolderIncreaseRowDTO(
        /** 变动股数（万股）；减持时接口可能为负或配合 {@link #direction} 理解 */
        @JsonProperty("CHANGE_NUM") BigDecimal changeNum,
        /** 公告日期（接口原样，常含 {@code 00:00:00}） */
        @JsonProperty("NOTICE_DATE") String noticeDate,
        /** 证券 6 位代码 */
        @JsonProperty("SECURITY_CODE") String securityCode,
        /** 股东名称 */
        @JsonProperty("HOLDER_NAME") String holderName,
        /** 变动后持股占总股本比例（%） */
        @JsonProperty("AFTER_CHANGE_RATE") BigDecimal afterChangeRate,
        /** 变动股数（万股，与 {@link #changeNum} 同口径，部分记录带符号） */
        @JsonProperty("CHANGE_NUM_SYMBOL") BigDecimal changeNumSymbol,
        /** 本次变动占总股本比例（%）；减持常为负 */
        @JsonProperty("CHANGE_RATE") BigDecimal changeRate,
        /** 变动截止日期（接口原样） */
        @JsonProperty("END_DATE") String endDate,
        /** 变动期间收盘价或披露收盘价（元） */
        @JsonProperty("CLOSE_PRICE") BigDecimal closePrice,
        /** 变动后持股数量（万股） */
        @JsonProperty("AFTER_HOLDER_NUM") BigDecimal afterHolderNum,
        /** 变动后持股比例（%） */
        @JsonProperty("HOLD_RATIO") BigDecimal holdRatio,
        /** 变动期间交易均价（元） */
        @JsonProperty("TRADE_AVERAGE_PRICE") BigDecimal tradeAveragePrice,
        /** 变动占自由流通股比例（%）；部分记录为空 */
        @JsonProperty("FREE_SHARES_RATIO") BigDecimal freeSharesRatio,
        /** 涉及自由流通股数量（万股）；部分记录为空 */
        @JsonProperty("FREE_SHARES") BigDecimal freeShares,
        /** 交易日期（接口原样） */
        @JsonProperty("TRADE_DATE") String tradeDate,
        /** 证券简称 */
        @JsonProperty("SECURITY_NAME_ABBR") String securityNameAbbr,
        /** 增减持方向，如「增持」「减持」 */
        @JsonProperty("DIRECTION") String direction,
        /** 东财数据入库时间 */
        @JsonProperty("EITIME") String eiTime,
        /** 变动占流通股比例（%） */
        @JsonProperty("CHANGE_FREE_RATIO") BigDecimal changeFreeRatio,
        /** 变动起始日期（接口原样） */
        @JsonProperty("START_DATE") String startDate,
        /** 实际成交价格（元）；大宗等场景可能为空 */
        @JsonProperty("REAL_PRICE") BigDecimal realPrice,
        /** 最新价（元，{@code quoteColumns} 行情联动） */
        @JsonProperty("NEWEST_PRICE") BigDecimal newestPrice,
        /** 最新涨跌幅（%，{@code quoteColumns} 行情联动） */
        @JsonProperty("CHANGE_RATE_QUOTES") BigDecimal changeRateQuotes,
        /** 变动途径/市场，如「二级市场」「大宗交易」 */
        @JsonProperty("MARKET") String market) {
}
