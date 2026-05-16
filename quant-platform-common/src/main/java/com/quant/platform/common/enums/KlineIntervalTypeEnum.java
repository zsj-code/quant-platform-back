package com.quant.platform.common.enums;

import java.util.Locale;

/**
 * K 线周期（与表字段 {@code interval_type} 一致），东财 {@code klt} 见各枚举常量的
 * {@link #getEastmoneyKlt()}。
 */
public enum KlineIntervalTypeEnum {

    /** 日 K */
    D("D", 101),
    /** 周 K */
    W("W", 102),
    /** 月 K */
    M("M", 103),
    /** 季 K */
    Q("Q", 104),
    /** 半年 K */
    HY("HY", 105),
    /** 年 K */
    Y("Y", 106),

    /** 1 分钟 */
    M1("M1", 1),
    /** 5 分钟 */
    M5("M5", 5),
    /** 15 分钟 */
    M15("M15", 15),
    /** 30 分钟 */
    M30("M30", 30),
    /** 60 分钟 */
    M60("M60", 60),
    /** 120 分钟 */
    M120("M120", 120);

    private final String code;
    private final int eastmoneyKlt;

    KlineIntervalTypeEnum(String code, int eastmoneyKlt) {
        this.code = code;
        this.eastmoneyKlt = eastmoneyKlt;
    }

    /** 落库 / API 使用的周期码 */
    public String getCode() {
        return code;
    }

    /** 东财 kline 接口的 klt 参数 */
    public int getEastmoneyKlt() {
        return eastmoneyKlt;
    }

    /**
     * 解析周期码，须与 {@link #getCode()} 完全一致（大小写不敏感）。
     *
     * @return 无法识别时返回 null
     */
    public static KlineIntervalTypeEnum fromCode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        for (KlineIntervalTypeEnum t : values()) {
            if (t.code.equals(v)) {
                return t;
            }
        }
        return null;
    }
}
