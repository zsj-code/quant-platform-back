package com.quant.platform.business.trader.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易决策单次运行（列表/分页展示，与表 {@code trader_decision_workflow_run} 对应）。
 */
@Data
public class TraderDecisionWorkflowRunVO {
    private String id;
    private String workflowRunKey;
    private String requestCodeRaw;
    private String secCode;
    private String sourceChannel;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    /** 完整 {@link TraderDecisionResult} JSON，可能较大 */
    private String decisionJson;
    private String llmSummaryText;
    private String errorMessage;
    /** 工作流步骤，按 {@link TraderDecisionWorkflowStepVO#getOrderIndex()} 升序 */
    private List<TraderDecisionWorkflowStepVO> steps;
}
