package com.quant.platform.business.trader.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易决策工作流单步（与单次运行多对一）。
 */
@Data
@TableName("trader_decision_workflow_step")
public class TraderDecisionWorkflowStepEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("workflow_run_id")
    private String workflowRunId;

    @TableField("step_key")
    private String stepKey;

    @TableField("step_name")
    private String stepName;

    @TableField("order_index")
    private Integer orderIndex;

    @TableField("status")
    private String status;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("duration_millis")
    private Long durationMillis;

    @TableField("detail")
    private String detail;

    @TableField("llm_system_prompt")
    private String llmSystemPrompt;

    @TableField("llm_user_prompt")
    private String llmUserPrompt;

    @TableField("llm_response_text")
    private String llmResponseText;
}
