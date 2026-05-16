package com.quant.platform.business.stock.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 股票退市状态，与表 stocks.is_delisted 一致：0-正常上市，1-已退市。
 */
public enum StockDelistStatus {

    /** 正常上市 */
    LISTED(0),
    /** 已退市 */
    DELISTED(1);

    @EnumValue
    private final int code;

    StockDelistStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static StockDelistStatus fromCode(int code) {
        for (StockDelistStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown StockDelistStatus code: " + code);
    }
}
