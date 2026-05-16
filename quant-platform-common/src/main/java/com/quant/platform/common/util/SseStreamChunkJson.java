package com.quant.platform.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.common.dto.SseStreamChunkDTO;

/**
 * {@link SseStreamChunkDTO} 与 JSON 互转（SSE {@code data} 行使用）。
 */
public final class SseStreamChunkJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SseStreamChunkJson() {
    }

    public static String toJson(SseStreamChunkDTO chunk) {
        try {
            return MAPPER.writeValueAsString(chunk);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SseStreamChunkDTO", e);
        }
    }

    public static String toJson(String content) {
        return toJson(SseStreamChunkDTO.of(content));
    }

    /**
     * 将流式 JSON 中的 {@code content} 追加到缓冲区（用于落库前拼接纯文本答案）。
     */
    public static void appendContent(StringBuilder buf, String content) {
        if (content != null && !content.isEmpty()) {
            buf.append(content);
        }
    }
}
