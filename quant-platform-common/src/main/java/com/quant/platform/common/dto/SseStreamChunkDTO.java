package com.quant.platform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SSE 流式增量事件（如 {@code delta}）的通用 JSON 载荷：客户端按 {@code content} 拼接展示；
 * 落库时只持久化各次 {@code content} 的拼接结果，不保存整段 JSON。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SseStreamChunkDTO(
        /** 本帧增量文本（可为多字符、含空格与换行；禁止为 null，空串表示空增量） */
        String content) {

    public SseStreamChunkDTO {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }

    public static SseStreamChunkDTO of(String content) {
        return new SseStreamChunkDTO(content);
    }
}
