package com.quant.platform.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 兼容 API 入参：项目默认 {@code yyyy-MM-dd HH:mm:ss}，同时接受 ISO-8601（含 {@code T}）与带时区字符串。
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private final DateTimeFormatter primary;

    FlexibleLocalDateTimeDeserializer(DateTimeFormatter primary) {
        this.primary = primary;
    }

    /**
     * Spring {@code Jackson2ObjectMapperBuilder#deserializers} 通过该方法解析目标类型；缺省为 null 会触发
     * {@code IllegalArgumentException: Unknown handled type}.
     */
    @Override
    public Class<?> handledType() {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, primary);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return ZonedDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException e) {
            throw ctxt.weirdStringException(
                    text,
                    LocalDateTime.class,
                    "expected '" + primary + "', ISO_LOCAL_DATE_TIME, or offset/zoned datetime");
        }
    }
}
