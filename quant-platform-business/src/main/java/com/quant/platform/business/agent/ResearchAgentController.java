package com.quant.platform.business.agent;

import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlanner;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/agent/research")
@ConditionalOnProperty(prefix = "quant.ai.langchain4j.openai", name = "api-key")
public class ResearchAgentController {

    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private final ObjectProvider<ResearchAgentService> service;
    private final ObjectProvider<ResearchPlanner> planner;
    private final ObjectProvider<ResearchSseService> sseService;

    public ResearchAgentController(ObjectProvider<ResearchAgentService> service,
                                   ObjectProvider<ResearchPlanner> planner,
                                   ObjectProvider<ResearchSseService> sseService) {
        this.service = service;
        this.planner = planner;
        this.sseService = sseService;
    }

    @PostMapping("/chat")
    public Result<ResearchChatResponse> chat(@RequestBody ResearchChatRequest req) {
        ResearchAgentService svc = service.getIfAvailable();
        if (svc == null) {
            return Result.fail(500, "Agent 未启用：请配置 quant.ai.langchain4j.openai.api-key");
        }
        if (req == null || req.getSessionId() == null || req.getSessionId().isBlank()) {
            return Result.fail(400, "sessionId 不能为空");
        }
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            return Result.fail(400, "message 不能为空");
        }
        ResearchAgentService.ResearchRunResult r = svc.chat(req.getSessionId(), req.getSymbol(), req.getMessage());
        return Result.ok(new ResearchChatResponse(r.getRunId(), r.getWorkflowRunKey(), r.getAnswer()));
    }

    @PostMapping("/plan")
    public Result<ResearchPlanResponse> plan(@RequestBody ResearchChatRequest req) {
        ResearchPlanner p = planner.getIfAvailable();
        if (p == null) {
            return Result.fail(500, "Agent 未启用：请配置 quant.ai.langchain4j.openai.api-key");
        }
        if (req == null || req.getMessage() == null || req.getMessage().isBlank()) {
            return Result.fail(400, "message 不能为空");
        }
        ResearchPlanner.PlanResult pr = p.plan(req.getSymbol(), req.getMessage());
        if (!pr.isOk()) {
            return Result.ok(ResearchPlanResponse.fail(pr.getError(), pr.getRawModelJson()));
        }
        return Result.ok(ResearchPlanResponse.ok(pr.getPlan(), pr.getRawModelJson()));
    }

    @PostMapping(value = "/chat-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatSse(@RequestBody ResearchChatRequest req) {
        ResearchSseService svc = sseService.getIfAvailable();
        if (svc == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data(
                        "Agent 未启用：请配置 quant.ai.langchain4j.openai.api-key", TEXT_PLAIN_UTF8));
            } catch (Exception ignored) {
                // ignored
            } finally {
                emitter.complete();
            }
            return emitter;
        }
        return svc.chat(req);
    }

    /**
     * SSE（GET 版）：用于浏览器原生 EventSource。
     * <p>
     * 注意：EventSource 只能 GET，参数通过 querystring 传入；message 需要 URL 编码。
     */
    @GetMapping(value = "/chat-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatSseGet(@RequestParam("sessionId") String sessionId,
                                 @RequestParam("code") String code,
                                 @RequestParam("message") String message) {
        ResearchSseService svc = sseService.getIfAvailable();
        if (svc == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data(
                        "Agent 未启用：请配置 quant.ai.langchain4j.openai.api-key", TEXT_PLAIN_UTF8));
            } catch (Exception ignored) {
                // ignored
            } finally {
                emitter.complete();
            }
            return emitter;
        }
        message = "标的：" + CommonUtil.toSymbol(code) + "," + message;
        ResearchChatRequest req = new ResearchChatRequest();
        req.setSessionId(sessionId);
        req.setSymbol(CommonUtil.toSymbol(code));
        req.setMessage(message);
        return svc.chat(req);
    }
}

