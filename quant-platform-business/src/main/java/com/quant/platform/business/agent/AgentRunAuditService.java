package com.quant.platform.business.agent;


import com.quant.platform.business.trader.entity.TraderDecisionWorkflowRunEntity;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowStepEntity;
import com.quant.platform.business.trader.mapper.TraderDecisionWorkflowRunMapper;
import com.quant.platform.business.trader.mapper.TraderDecisionWorkflowStepMapper;
import com.quant.platform.common.enums.TraderDecisionWorkflowRunStatus;

import com.quant.platform.common.constant.TraderDecisionWorkflowRunKeys;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 复用 trader_decision_workflow_run/step 表做 Agent Run 审计落库。
 */
@Service
public class AgentRunAuditService {
    public static final String SOURCE_CHANNEL_AGENT_RESEARCH = "AGENT_RESEARCH";

    private final TraderDecisionWorkflowRunMapper runMapper;
    private final TraderDecisionWorkflowStepMapper stepMapper;

    public AgentRunAuditService(TraderDecisionWorkflowRunMapper runMapper,
                                TraderDecisionWorkflowStepMapper stepMapper) {
        this.runMapper = runMapper;
        this.stepMapper = stepMapper;
    }

    public TraderDecisionWorkflowRunEntity startRun(String requestCodeOrSymbol) {
        String runKey = TraderDecisionWorkflowRunKeys.newRunKey(requestCodeOrSymbol);
        String secCode = CommonUtil.normalizeSixDigitCode(requestCodeOrSymbol);

        TraderDecisionWorkflowRunEntity e = new TraderDecisionWorkflowRunEntity();
        e.setWorkflowRunKey(runKey);
        e.setRequestCodeRaw(requestCodeOrSymbol);
        e.setSecCode(secCode);
        e.setSourceChannel(SOURCE_CHANNEL_AGENT_RESEARCH);
        e.setStatus(TraderDecisionWorkflowRunStatus.RUNNING.name());
        e.setStartedAt(LocalDateTime.now());

        runMapper.insert(e);
        return e;
    }

    public void upsertStep(String runId,
                           String stepKey,
                           String stepName,
                           int orderIndex,
                           String status,
                           LocalDateTime startedAt,
                           LocalDateTime finishedAt,
                           Long durationMillis,
                           String detail,
                           String llmSystemPrompt,
                           String llmUserPrompt,
                           String llmResponseText) {
        TraderDecisionWorkflowStepEntity e = new TraderDecisionWorkflowStepEntity();
        e.setWorkflowRunId(runId);
        e.setStepKey(stepKey);
        e.setStepName(stepName);
        e.setOrderIndex(orderIndex);
        e.setStatus(status);
        e.setStartedAt(startedAt);
        e.setFinishedAt(finishedAt);
        e.setDurationMillis(durationMillis);
        e.setDetail(detail);
        e.setLlmSystemPrompt(llmSystemPrompt);
        e.setLlmUserPrompt(llmUserPrompt);
        e.setLlmResponseText(llmResponseText);
        stepMapper.upsertStep(e);
    }

    public void markSucceeded(String workflowRunKey, String llmSummaryText) {
        runMapper.updateLlmSummaryAndFinish(workflowRunKey, llmSummaryText);
    }

    public void markFailed(String workflowRunKey, String errorMessage) {
        runMapper.updateStatusAndFinished(workflowRunKey, TraderDecisionWorkflowRunStatus.FAILED.name(), errorMessage);
    }
}

