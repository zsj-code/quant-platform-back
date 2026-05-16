package com.quant.platform.business.agent;

import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlanner;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.util.CommonUtil;
import org.apache.commons.lang3.StringUtils;
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
    private final ObjectProvider<ResearchAgentRunManager> runManager;

    public ResearchAgentController(ObjectProvider<ResearchAgentService> service,
                                   ObjectProvider<ResearchPlanner> planner,
                                   ObjectProvider<ResearchSseService> sseService,
                                   ObjectProvider<ResearchAgentRunManager> runManager) {
        this.service = service;
        this.planner = planner;
        this.sseService = sseService;
        this.runManager = runManager;
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
        try {
            ResearchAgentService.ResearchRunResult r = svc.chat(req.getSessionId(), req.getSymbol(), req.getMessage());
            return Result.ok(new ResearchChatResponse(r.getRunId(), r.getWorkflowRunKey(), r.getAnswer()));
        } catch (ResearchAgentCancelledException ex) {
            return Result.fail(499, ex.getMessage());
        }
    }

    /**
     * 停止当前会话下进行中的 Research Agent（规划 / 工具 / 大模型流式或阻塞调用）。
     * <p>
     * {@code sessionId} 与 chat 一致；可选 {@code runId}（SSE meta 事件中的 runId）。
     */
    @PostMapping("/stop")
    public Result<ResearchStopResponse> stop(@RequestBody ResearchStopRequest req) {
        ResearchAgentRunManager manager = runManager.getIfAvailable();
        if (manager == null) {
            return Result.fail(500, "Agent 未启用：请配置 quant.ai.langchain4j.openai.api-key");
        }
        if (req == null || (isBlank(req.getSessionId()) && isBlank(req.getRunId()))) {
            return Result.fail(400, "sessionId 与 runId 至少填一个");
        }
        boolean stopped = manager.stop(req.getSessionId(), req.getRunId(), "用户停止");
        if (!stopped) {
            return Result.fail(404, "没有进行中的任务");
        }
        return Result.ok(new ResearchStopResponse(true, req.getSessionId(), req.getRunId()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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
                                 @RequestParam(name = "message", required =false) String message) {
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
        if (StringUtils.isEmpty(message)) {
            message = "标的：" + CommonUtil.toSymbol(code) + "，帮我做一次基本面+技术面+情绪面综合评估，并给出风险点。";
        } else {
            message = "标的：" + CommonUtil.toSymbol(code) + message;
        }
        ResearchChatRequest req = new ResearchChatRequest();
        req.setSessionId(sessionId);
        req.setSymbol(CommonUtil.toSymbol(code));
        req.setMessage(message);
        return svc.chat(req);
    }
}

