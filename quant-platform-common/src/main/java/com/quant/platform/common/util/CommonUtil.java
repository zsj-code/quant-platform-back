package com.quant.platform.common.util;

import java.util.Locale;

public final class CommonUtil {
    private CommonUtil() {
    }

    public static String toCode(String symbol) {
        return symbol.substring(0, symbol.indexOf("."));
    }

    /**
     * 纯数字 code → 带交易所后缀的 symbol（无 exchange 字段时按 A 股常见号段推断）。
     */
    public static String toSymbol(String code) {
        if (code == null)
            return null;
        String c = code.trim();
        if (c.isEmpty())
            return null;

        String suffix;
        if (c.startsWith("6") || c.startsWith("688") || c.startsWith("689")) {
            suffix = "SH";
        } else if (c.startsWith("0") || c.startsWith("1") || c.startsWith("2") || c.startsWith("3")) {
            suffix = "SZ";
        } else if (c.startsWith("4") || c.startsWith("8")) {
            suffix = "BJ";
        } else {
            suffix = "SZ";
        }

        return c + "." + suffix;
    }

    /**
     * 纯数字 code → 东方财富 {@code secid}（与 {@link #toSymbol(String)} + {@link #toSecIdFromSymbol(String)} 一致）。
     * <p>
     * 规则：沪市 {@code 1.xxxxxx}，深市与北交所 {@code 0.xxxxxx}。
     */
    public static String toSecId(String code) {
        String symbol = toSymbol(code);
        return symbol == null ? null : toSecIdFromSymbol(symbol);
    }

    /**
     * symbol → secid，例如 {@code 600000.SH} → {@code 1.600000}。
     */
    public static String toSecIdFromSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }
        String v = symbol.trim().toUpperCase(Locale.ROOT);
        int idx = v.indexOf('.');
        if (idx <= 0 || idx == v.length() - 1) {
            return null;
        }
        String code = v.substring(0, idx);
        String suffix = v.substring(idx + 1);
        String mkt = "SH".equals(suffix) ? "1" : "0";
        return mkt + "." + code;
    }

    /**
     * 提取 6 位证券代码（去掉 {@code .SH} / {@code .SZ} 等后缀）。
     */
    public static String normalizeSixDigitCode(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim();
        int dot = c.indexOf('.');
        if (dot > 0) {
            c = c.substring(0, dot);
        }
        return c;
    }

    /**
     * 6 位 code → 淘股吧行情页全码小写，如 {@code sh600000}、{@code sz000001}（与 PC {@code /quotes/} 路径一致；按交易所前缀 + 代码拼接再小写）。
     */
    public static String toTaogubaFullCode(String code) {
        String sym = toSymbol(code);
        if (sym == null) {
            return null;
        }
        int dot = sym.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        String exchPrefix = sym.substring(dot + 1) + sym.substring(0, dot);
        return exchPrefix.toLowerCase(Locale.ROOT);
    }
}
