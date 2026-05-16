package com.quant.platform.common.enums;

import java.util.Objects;

public enum FinancialReportTypeEnum {
    INCOME("INCOME", "利润表"),
    BALANCE("BALANCE", "资产负债表"),
    CASHFLOW("CASHFLOW", "现金流量表");

    private final String code;
    private final String desc;

    FinancialReportTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static FinancialReportTypeEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String c = code.trim();
        for (FinancialReportTypeEnum v : values()) {
            if (Objects.equals(v.code, c)) {
                return v;
            }
        }
        return null;
    }
}
