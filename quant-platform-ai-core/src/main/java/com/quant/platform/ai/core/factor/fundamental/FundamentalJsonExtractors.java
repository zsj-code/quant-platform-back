package com.quant.platform.ai.core.factor.fundamental;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

public final class FundamentalJsonExtractors {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private FundamentalJsonExtractors() {
    }

    public static Map<String, Object> parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(rawJson, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static BigDecimal pickDecimal(Map<String, Object> map, String... keys) {
        if (map == null || map.isEmpty() || keys == null) {
            return null;
        }
        for (String k : keys) {
            if (k == null || k.isBlank()) {
                continue;
            }
            Object v = map.get(k);
            BigDecimal d = toDecimal(v);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    public static BigDecimal toDecimal(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v instanceof Number) {
            return BigDecimal.valueOf(((Number) v).doubleValue());
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s) || "--".equals(s)) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }
}

