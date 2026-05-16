package com.quant.platform.business.kline.dto;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class EastmoneyKlineBarDTO {
    String date; // f51
    BigDecimal open; // f52
    BigDecimal close; // f53
    BigDecimal high; // f54
    BigDecimal low; // f55
    Long volume; // f56
    /**
     * 成交额
     */
    BigDecimal amount; // f57
    /** 振幅 % */
    BigDecimal amplitude; // f58
    /** 涨跌幅 % */
    BigDecimal changePct; // f59
    /** 涨跌额 */
    BigDecimal changeAmount; // f60
    /** 换手率 % */
    BigDecimal turnoverRate; // f61
}
