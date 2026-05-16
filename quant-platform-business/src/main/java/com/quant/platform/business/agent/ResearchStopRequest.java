package com.quant.platform.business.agent;

import lombok.Data;

@Data
public class ResearchStopRequest {
    /** 与发起 chat / chat-sse 时一致的会话 id（优先） */
    private String sessionId;
    /** 可选：meta 事件返回的 runId */
    private String runId;
}
