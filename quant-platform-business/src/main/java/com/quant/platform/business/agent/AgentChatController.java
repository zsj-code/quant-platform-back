package com.quant.platform.business.agent;

import com.quant.platform.ai.core.langchain4j.LangChain4jOpenAiProperties;
import com.quant.platform.ai.core.langchain4j.QuantAiAgentService;
import com.quant.platform.ai.core.langchain4j.QuantAiAssistant;
import com.quant.platform.ai.core.langchain4j.QuantAiStreamingAssistant;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 最小联调入口：验证 LangChain4j Agent 是否可用。
 */
@RestController
@RequestMapping("/api/agent")
@ConditionalOnProperty(prefix = "quant.ai.langchain4j.openai", name = "api-key")
public class AgentChatController {
    private final ObjectProvider<QuantAiAgentService> agentService;
    private final ObjectProvider<LangChain4jOpenAiProperties> props;
    private final ObjectProvider<QuantAiAssistant> assistant;
    private final ObjectProvider<QuantAiStreamingAssistant> streamingAssistant;

    public AgentChatController(ObjectProvider<QuantAiAgentService> agentService,
                               ObjectProvider<LangChain4jOpenAiProperties> props,
                               ObjectProvider<QuantAiAssistant> assistant,
                               ObjectProvider<QuantAiStreamingAssistant> streamingAssistant) {
        this.agentService = agentService;
        this.props = props;
        this.assistant = assistant;
        this.streamingAssistant = streamingAssistant;
    }

    @GetMapping("/chat")
    public Result<String> chat(@RequestParam("sessionId") String sessionId,
                              @RequestParam("message") String message) {
        QuantAiAgentService svc = agentService.getIfAvailable();
        if (svc == null) {
            return Result.fail(500, "Agent 未启用：请配置 quant.ai.langchain4j.openai.api-key");
        }
        return Result.ok(svc.chat(sessionId, message));
    }

}

