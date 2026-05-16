package com.quant.platform.business.trader.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单次运行下的工作流一步（与表 {@code trader_decision_workflow_step} 对应；列表按 {@link #orderIndex} 升序）。
 */
@Data
public class TraderDecisionWorkflowStepVO {
    private String id;
    private String stepKey;
    private String stepName;
    private Integer orderIndex;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMillis;
    private String detail;
    /** 该步骤 LLM system 提示（未调用为 null） */
    private String llmSystemPrompt;
    /** 该步骤 LLM user 输入 */
    private String llmUserPrompt;
    /** 该步骤 LLM 完整输出 */
    private String llmResponseText;
}
