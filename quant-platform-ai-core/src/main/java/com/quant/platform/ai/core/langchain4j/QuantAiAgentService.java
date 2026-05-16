package com.quant.platform.ai.core.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * 对外的 Agent 门面：屏蔽 LangChain4j 细节，便于后续替换/扩展（规划器、多 Agent 等）。
 */
@Service
@ConditionalOnBean(QuantAiAssistant.class)
public class QuantAiAgentService {
    private final QuantAiAssistant assistant;

    public QuantAiAgentService(QuantAiAssistant assistant) {
        this.assistant = assistant;
    }

    public String chat(String sessionId, String message) {
        return assistant.chat(sessionId, message);
    }
}

