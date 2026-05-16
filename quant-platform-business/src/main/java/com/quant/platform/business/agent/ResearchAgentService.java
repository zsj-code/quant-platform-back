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

@Service
@ConditionalOnBean(QuantAiAgentService.class)
public class ResearchAgentService {
    private final QuantAiAgentService agent;
    private final AgentRunAuditService audit;
    private final ResearchPlanner planner;
    private final ToolRouter toolRouter;

    public ResearchAgentService(QuantAiAgentService agent,
                                AgentRunAuditService audit,
                                ResearchPlanner planner,
                                ToolRouter toolRouter) {
        this.agent = agent;
        this.audit = audit;
        this.planner = planner;
        this.toolRouter = toolRouter;
    }

    public ResearchRunResult chat(String sessionId, String symbol, String message) {
        String anchor = (symbol != null && !symbol.isBlank()) ? symbol : message;
        TraderDecisionWorkflowRunEntity run = audit.startRun(anchor);

        // step 1: plan
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

        // step 2..n: execute tools (best-effort). 失败不直接中断，交给最终回答解释缺失。
        if (pr.isOk() && pr.getPlan() != null && pr.getPlan().getSteps() != null) {
            int idx = 0;
            for (ResearchPlan.Step s : pr.getPlan().getSteps()) {
                if (s == null || s.getTool() == null || s.getTool().isBlank()) {
                    continue;
                }
                idx++;
                LocalDateTime ts = LocalDateTime.now();
                long t0 = System.currentTimeMillis();
                String stepKey = "tool:" + idx + ":" + s.getTool();
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

        // final: answer (with memory + tools loop managed by LangChain4j)
        LocalDateTime started = LocalDateTime.now();
        long t0 = System.currentTimeMillis();
        try {
            String finalMessage = message;
            if (symbol != null && !symbol.isBlank()) {
                finalMessage = "标的(symbol)=" + symbol + "\n" + message;
            }
            String answer = agent.chat(sessionId, finalMessage);
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
                    finalMessage,
                    answer);
            audit.markSucceeded(run.getWorkflowRunKey(), answer);
            return new ResearchRunResult(run.getId(), run.getWorkflowRunKey(), answer);
        } catch (Exception e) {
            LocalDateTime finished = LocalDateTime.now();
            long dur = Math.max(0L, System.currentTimeMillis() - t0);
            audit.upsertStep(run.getId(),
                    "answer",
                    "最终回答",
                    100,
                    "FAILED",
                    started,
                    finished,
                    dur,
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

