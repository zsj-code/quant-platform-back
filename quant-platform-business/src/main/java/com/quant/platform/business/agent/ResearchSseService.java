package com.quant.platform.business.agent;

import com.quant.platform.ai.core.langchain4j.QuantAiStreamingAssistant;
import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlan;
import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlanner;
import com.quant.platform.ai.core.langchain4j.research.plan.ToolRouter;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowRunEntity;
import com.quant.platform.common.util.SseStreamChunkJson;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(prefix = "quant.ai.langchain4j.openai", name = "api-key")
@Slf4j
public class ResearchSseService {

    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private static final MediaType APPLICATION_JSON_UTF8 =
        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private final ObjectProvider<QuantAiStreamingAssistant> streamingAssistant;
    private final ResearchAgentService blockingResearchAgent;
    private final AgentRunAuditService audit;
    private final ResearchPlanner planner;
    private final ToolRouter toolRouter;
    private final ResearchAgentRunManager runManager;

    public ResearchSseService(ObjectProvider<QuantAiStreamingAssistant> streamingAssistant,
                              ResearchAgentService blockingResearchAgent,
                              AgentRunAuditService audit,
                              ResearchPlanner planner,
                              ToolRouter toolRouter,
                              ResearchAgentRunManager runManager) {
        this.streamingAssistant = streamingAssistant;
        this.blockingResearchAgent = blockingResearchAgent;
        this.audit = audit;
        this.planner = planner;
        this.toolRouter = toolRouter;
        this.runManager = runManager;
    }

    public SseEmitter chat(ResearchChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = req == null ? null : req.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            emitErrorAndComplete(emitter, "sessionId 不能为空");
            return emitter;
        }
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            emitErrorAndComplete(emitter, "message 不能为空");
            return emitter;
        }

        ResearchAgentRunManager.ActiveRun ctx = runManager.register(sessionId);
        ctx.attachSse(emitter);

        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            ctx.bindWorkerThread();
            try {
                runSse(ctx, emitter, req);
            } catch (ResearchAgentCancelledException ex) {
                log.info("research sse cancelled sessionId={}", sessionId);
            } catch (Exception e) {
                try {
                    send(emitter, "error", e.toString());
                } catch (Exception ignored) {
                    // ignored
                }
                emitter.completeWithError(e);
            } finally {
                runManager.remove(ctx);
            }
        }, runManager.researchExecutor());
        ctx.attachTask(task);

        return emitter;
    }

    private void runSse(ResearchAgentRunManager.ActiveRun ctx, SseEmitter emitter, ResearchChatRequest req)
        throws Exception {
        String sessionId = req.getSessionId();
        String message = req.getMessage();
        String symbol = req.getSymbol();

        ctx.checkCancelled();
        String anchor = (symbol != null && !symbol.isBlank()) ? symbol : message;
        TraderDecisionWorkflowRunEntity run = audit.startRun(anchor);
        runManager.bindRunMeta(ctx, run.getId(), run.getWorkflowRunKey());

        send(emitter, "meta",
            "{\"runId\":\"" + run.getId() + "\",\"workflowRunKey\":\"" + run.getWorkflowRunKey() + "\"}");

        // plan
        send(emitter, "stage", "plan:start");
        ctx.checkCancelled();
        LocalDateTime planStart = LocalDateTime.now();
        long planT0 = System.currentTimeMillis();
        ResearchPlanner.PlanResult pr = planner.plan(symbol, message);
        ctx.checkCancelled();
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

        // tools
        if (pr.isOk() && pr.getPlan() != null && pr.getPlan().getSteps() != null) {
            int idx = 0;
            for (ResearchPlan.Step s : pr.getPlan().getSteps()) {
                ctx.checkCancelled();
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
                    ctx.checkCancelled();
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
                } catch (ResearchAgentCancelledException ex) {
                    throw ex;
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

        // answer
        send(emitter, "stage", "answer:start");
        final LocalDateTime started = LocalDateTime.now();
        final long t0 = System.currentTimeMillis();
        final String runId = run.getId();
        final String workflowRunKey = run.getWorkflowRunKey();
        final String auditMessage = message;

        ctx.checkCancelled();
        QuantAiStreamingAssistant assistant = streamingAssistant.getIfAvailable();
        if (assistant == null) {
            streamAnswerFallback(ctx, emitter, sessionId, symbol, message, run, started, t0, auditMessage);
            return;
        }

        StringBuilder answerBuf = new StringBuilder();
        TokenStream stream = assistant.chat(sessionId, message);
        stream.onPartialResponse(token -> {
                if (ctx.isCancelled()) {
                    return;
                }
                String chunk = token == null ? "" : token;
                SseStreamChunkJson.appendContent(answerBuf, chunk);
                sendDelta(emitter, chunk);
            })
            .onCompleteResponse(response -> {
                if (ctx.isCancelled()) {
                    return;
                }
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
                if (ctx.isCancelled()) {
                    return;
                }
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
    }

    private void streamAnswerFallback(ResearchAgentRunManager.ActiveRun ctx,
                                        SseEmitter emitter,
                                        String sessionId,
                                        String symbol,
                                        String message,
                                        TraderDecisionWorkflowRunEntity run,
                                        LocalDateTime started,
                                        long t0,
                                        String auditMessage) throws Exception {
        ctx.checkCancelled();
        ResearchAgentService.ResearchRunResult r = blockingResearchAgent.chat(sessionId, symbol, message);
        ctx.checkCancelled();
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
        audit.markSucceeded(run.getWorkflowRunKey(), answer);
        send(emitter, "stage", "answer:done");
        send(emitter, "done", "");
        emitter.complete();
    }

    private static void emitErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message, TEXT_PLAIN_UTF8));
        } catch (Exception ignored) {
            // ignored
        } finally {
            emitter.complete();
        }
    }

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
