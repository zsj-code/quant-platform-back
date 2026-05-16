package com.quant.platform.business.agent;

import com.quant.platform.ai.core.langchain4j.QuantAiAgentService;
import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlan;
import com.quant.platform.ai.core.langchain4j.research.plan.ResearchPlanner;
import com.quant.platform.ai.core.langchain4j.research.plan.ToolRouter;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowRunEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@ConditionalOnBean(QuantAiAgentService.class)
public class ResearchAgentService {
    private final QuantAiAgentService agent;
    private final AgentRunAuditService audit;
    private final ResearchPlanner planner;
    private final ToolRouter toolRouter;
    private final ResearchAgentRunManager runManager;

    public ResearchAgentService(QuantAiAgentService agent,
                                AgentRunAuditService audit,
                                ResearchPlanner planner,
                                ToolRouter toolRouter,
                                ResearchAgentRunManager runManager) {
        this.agent = agent;
        this.audit = audit;
        this.planner = planner;
        this.toolRouter = toolRouter;
        this.runManager = runManager;
    }

    public ResearchRunResult chat(String sessionId, String symbol, String message) {
        ResearchAgentRunManager.ActiveRun ctx = runManager.register(sessionId);
        CompletableFuture<ResearchRunResult> future = CompletableFuture.supplyAsync(() -> {
            ctx.bindWorkerThread();
            try {
                return execute(ctx, sessionId, symbol, message);
            } finally {
                runManager.remove(ctx);
            }
        }, runManager.researchExecutor());
        ctx.attachTask(future);
        try {
            return future.get();
        } catch (CancellationException ex) {
            throw new ResearchAgentCancelledException("用户已停止当前任务");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResearchAgentCancelledException("用户已停止当前任务");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ResearchAgentCancelledException cancelled) {
                throw cancelled;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(cause);
        }
    }

    private ResearchRunResult execute(ResearchAgentRunManager.ActiveRun ctx, String sessionId, String symbol,
                                      String message) {
        ctx.checkCancelled();
        String anchor = (symbol != null && !symbol.isBlank()) ? symbol : message;
        TraderDecisionWorkflowRunEntity run = audit.startRun(anchor);
        runManager.bindRunMeta(ctx, run.getId(), run.getWorkflowRunKey());

        try {
            // step 1: plan
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

            // step 2..n: tools
            if (pr.isOk() && pr.getPlan() != null && pr.getPlan().getSteps() != null) {
                int idx = 0;
                for (ResearchPlan.Step s : pr.getPlan().getSteps()) {
                    ctx.checkCancelled();
                    if (s == null || s.getTool() == null || s.getTool().isBlank()) {
                        continue;
                    }
                    idx++;
                    LocalDateTime ts = LocalDateTime.now();
                    long t0 = System.currentTimeMillis();
                    String stepKey = "tool:" + idx + ":" + s.getTool();
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
                    }
                    if (idx >= 6) {
                        break;
                    }
                }
            }

            // final: answer
            ctx.checkCancelled();
            LocalDateTime started = LocalDateTime.now();
            long t0 = System.currentTimeMillis();
            String answer = agent.chat(sessionId, message);
            ctx.checkCancelled();
            LocalDateTime finished = LocalDateTime.now();
            long dur = Math.max(0L, System.currentTimeMillis() - t0);

            audit.upsertStep(run.getId(),
                "answer",
                "最终回答",
                100,
                "SUCCEEDED",
                started,
                finished,
                dur,
                null,
                null,
                message,
                answer);
            audit.markSucceeded(run.getWorkflowRunKey(), answer);
            return new ResearchRunResult(run.getId(), run.getWorkflowRunKey(), answer);
        } catch (ResearchAgentCancelledException ex) {
            if (run.getWorkflowRunKey() != null) {
                audit.markCancelled(run.getWorkflowRunKey(), ex.getMessage());
            }
            throw ex;
        } catch (Exception e) {
            LocalDateTime finished = LocalDateTime.now();
            audit.upsertStep(run.getId(),
                "answer",
                "最终回答",
                100,
                "FAILED",
                LocalDateTime.now(),
                finished,
                0L,
                e.toString(),
                null,
                message,
                null);
            audit.markFailed(run.getWorkflowRunKey(), e.toString());
            throw e;
        }
    }

    public static final class ResearchRunResult {
        private final String runId;
        private final String workflowRunKey;
        private final String answer;

        public ResearchRunResult(String runId, String workflowRunKey, String answer) {
            this.runId = runId;
            this.workflowRunKey = workflowRunKey;
            this.answer = answer;
        }

        public String getRunId() {
            return runId;
        }

        public String getWorkflowRunKey() {
            return workflowRunKey;
        }

        public String getAnswer() {
            return answer;
        }
    }
}
