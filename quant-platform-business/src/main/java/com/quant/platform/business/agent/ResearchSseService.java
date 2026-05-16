package com.quant.platform.business.agent;


import com.quant.platform.ai.core.langchain4j.QuantAiStreamingAssistant;
import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlan;
import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlanner;
import com.quant.platform.ai.core.langchain4j.research.plan.ToolRouter;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowRunEntity;
import com.quant.platform.common.util.SseStreamChunkJson;
import dev.langchain4j.service.TokenStream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
@ConditionalOnProperty(prefix = "quant.ai.langchain4j.openai", name = "api-key")
public class ResearchSseService {

    private static final Executor SSE_EXECUTOR = Executors.newCachedThreadPool();

    /** 非 delta 事件（stage / error 等）仍用 UTF-8 纯文本。 */
    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private static final MediaType APPLICATION_JSON_UTF8 =
            new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private final ObjectProvider<QuantAiStreamingAssistant> streamingAssistant;
    private final ResearchAgentService blockingResearchAgent;
    private final AgentRunAuditService audit;
    private final ResearchPlanner planner;
    private final ToolRouter toolRouter;

    public ResearchSseService(ObjectProvider<QuantAiStreamingAssistant> streamingAssistant,
                              ResearchAgentService blockingResearchAgent,
                              AgentRunAuditService audit,
                              ResearchPlanner planner,
                              ToolRouter toolRouter) {
        this.streamingAssistant = streamingAssistant;
        this.blockingResearchAgent = blockingResearchAgent;
        this.audit = audit;
        this.planner = planner;
        this.toolRouter = toolRouter;
    }

    public SseEmitter chat(ResearchChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                String sessionId = req == null ? null : req.getSessionId();
                String message = req == null ? null : req.getMessage();
                String symbol = req == null ? null : req.getSymbol();

                if (sessionId == null || sessionId.isBlank()) {
                    send(emitter, "error", "sessionId 不能为空");
                    emitter.complete();
                    return;
                }
                if (message == null || message.isBlank()) {
                    send(emitter, "error", "message 不能为空");
                    emitter.complete();
                    return;
                }

                String anchor = (symbol != null && !symbol.isBlank()) ? symbol : message;
                TraderDecisionWorkflowRunEntity run = audit.startRun(anchor);

                send(emitter, "meta",
                        "{\"runId\":\"" + run.getId() + "\",\"workflowRunKey\":\"" + run.getWorkflowRunKey() + "\"}");

                // step 1: plan
                send(emitter, "stage", "plan:start");
                LocalDateTime planStart = LocalDateTime.now();
                long planT0 = System.currentTimeMillis();
                ResearchPlanner.PlanResult pr = planner.plan(symbol, message);
                LocalDateTime planFinish = LocalDateTime.now();
                long planDur = Math.max(0L, System.currentTimeMillis() - planT0);
                audit.upsertStep(run.getId(),
                        "plan",
                        "生成计划",
                        10,
                        pr.isOk() ? "SUCCEEDED" : "FAILED",
                        planStart,
                        planFinish,
                        planDur,
                        pr.isOk() ? null : pr.getError(),
                        null,
                        pr.getPlannerPrompt(),
                        pr.getRawModelJson());
                send(emitter, "stage", pr.isOk() ? "plan:done" : "plan:failed");

                // step 2..n: execute tools (best-effort)
                if (pr.isOk() && pr.getPlan() != null && pr.getPlan().getSteps() != null) {
                    int idx = 0;
                    for (ResearchPlan.Step s : pr.getPlan().getSteps()) {
                        if (s == null || s.getTool() == null || s.getTool().isBlank()) {
                            continue;
                        }
                        idx++;
                        if (idx > 6) {
                            break;
                        }

                        String stepKey = "tool:" + idx + ":" + s.getTool();
                        send(emitter, "stage", stepKey + ":start");
                        LocalDateTime ts = LocalDateTime.now();
                        long t0 = System.currentTimeMillis();
                        try {
                            Map<String, Object> out = toolRouter.call(s.getTool(), s.getArgs());
                            LocalDateTime tf = LocalDateTime.now();
                            long dur = Math.max(0L, System.currentTimeMillis() - t0);
                            audit.upsertStep(run.getId(),
                                    stepKey,
                                    "工具调用 " + s.getTool(),
                                    20 + idx,
                                    "SUCCEEDED",
                                    ts,
                                    tf,
                                    dur,
                                    null,
                                    null,
                                    String.valueOf(s.getArgs()),
                                    String.valueOf(out));
                            send(emitter, "stage", stepKey + ":done");
                        } catch (Exception ex) {
                            LocalDateTime tf = LocalDateTime.now();
                            long dur = Math.max(0L, System.currentTimeMillis() - t0);
                            audit.upsertStep(run.getId(),
                                    stepKey,
                                    "工具调用 " + s.getTool(),
                                    20 + idx,
                                    "FAILED",
                                    ts,
                                    tf,
                                    dur,
                                    ex.toString(),
                                    null,
                                    String.valueOf(s.getArgs()),
                                    null);
                            send(emitter, "stage", stepKey + ":failed");
                        }
                    }
                }

                // final: stream answer
                String finalMessage = message;
                if (symbol != null && !symbol.isBlank()) {
                    finalMessage = "标的(symbol)=" + symbol + "\n" + message;
                }

                send(emitter, "stage", "answer:start");
                final LocalDateTime started = LocalDateTime.now();
                final long t0 = System.currentTimeMillis();
                final String runId = run.getId();
                final String workflowRunKey = run.getWorkflowRunKey();
                final String auditMessage = finalMessage;

                QuantAiStreamingAssistant assistant = streamingAssistant.getIfAvailable();
                if (assistant == null) {
                    // 降级：仍然用 SSE 返回，但只推送一次最终答案（避免 ObjectProvider 为 null）
                    ResearchAgentService.ResearchRunResult r = blockingResearchAgent.chat(sessionId, symbol, message);
                    String answer = r.getAnswer() == null ? "" : r.getAnswer();
                    sendDelta(emitter, answer);
                    audit.upsertStep(run.getId(),
                            "answer",
                            "最终回答",
                            100,
                            "SUCCEEDED",
                            started,
                            LocalDateTime.now(),
                            Math.max(0L, System.currentTimeMillis() - t0),
                            null,
                            null,
                            auditMessage,
                            answer);
                    audit.markSucceeded(workflowRunKey, answer);
                    send(emitter, "stage", "answer:done");
                    send(emitter, "done", "");
                    emitter.complete();
                    return;
                }

                StringBuilder answerBuf = new StringBuilder();
                TokenStream stream = assistant.chat(sessionId, finalMessage);
                stream.onPartialResponse(token -> {
                            String chunk = token == null ? "" : token;
                            SseStreamChunkJson.appendContent(answerBuf, chunk);
                            sendDelta(emitter, chunk);
                        })
                        .onCompleteResponse(response -> {
                            LocalDateTime finished = LocalDateTime.now();
                            long dur = Math.max(0L, System.currentTimeMillis() - t0);
                            String answer = answerBuf.toString();

                            audit.upsertStep(runId,
                                    "answer",
                                    "最终回答",
                                    100,
                                    "SUCCEEDED",
                                    started,
                                    finished,
                                    dur,
                                    null,
                                    null,
                                    auditMessage,
                                    answer);
                            audit.markSucceeded(workflowRunKey, answer);

                            send(emitter, "stage", "answer:done");
                            send(emitter, "done", "");
                            emitter.complete();
                        })
                        .onError(err -> {
                            LocalDateTime finished = LocalDateTime.now();
                            long dur = Math.max(0L, System.currentTimeMillis() - t0);
                            audit.upsertStep(runId,
                                    "answer",
                                    "最终回答",
                                    100,
                                    "FAILED",
                                    started,
                                    finished,
                                    dur,
                                    String.valueOf(err),
                                    null,
                                    auditMessage,
                                    null);
                            audit.markFailed(workflowRunKey, String.valueOf(err));

                            send(emitter, "error", String.valueOf(err));
                            emitter.completeWithError(err);
                        })
                        .start();
            } catch (Exception e) {
                try {
                    send(emitter, "error", e.toString());
                } catch (Exception ignored) {
                    // ignored
                } finally {
                    emitter.completeWithError(e);
                }
            }
        }, SSE_EXECUTOR);

        return emitter;
    }

    /**
     * 流式答案增量：SSE {@code data} 为 {@link com.quant.platform.common.dto.SseStreamChunkDTO} 的 JSON，仅 {@code content} 参与拼接落库。
     */
    private static void sendDelta(SseEmitter emitter, String content) {
        try {
            String json = SseStreamChunkJson.toJson(content == null ? "" : content);
            emitter.send(SseEmitter.event().name("delta").data(json, APPLICATION_JSON_UTF8));
        } catch (Exception ignored) {
            // ignored
        }
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            if (data instanceof String s) {
                emitter.send(SseEmitter.event().name(event).data(s, TEXT_PLAIN_UTF8));
            } else {
                emitter.send(SseEmitter.event().name(event).data(data));
            }
        } catch (Exception ignored) {
            // ignored
        }
    }
}

