package com.quant.platform.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 默认 {@code yyyy-MM-dd}，同时接受 ISO 日期与日期时间字符串（取日历日部分）。
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private final DateTimeFormatter primary;
    private final DateTimeFormatter dateTimePrimary;

    FlexibleLocalDateDeserializer(DateTimeFormatter primary, DateTimeFormatter dateTimePrimary) {
        this.primary = primary;
        this.dateTimePrimary = dateTimePrimary;
    }

    @Override
    public Class<?> handledType() {
        return LocalDate.class;
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null) {
            return null;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text, primary);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(text, dateTimePrimary).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(text).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return ZonedDateTime.parse(text).toLocalDate();
        } catch (DateTimeParseException e) {
            throw ctxt.weirdStringException(
                    text,
                    LocalDate.class,
                    "expected date, or datetime convertible to LocalDate");
        }
    }
}
