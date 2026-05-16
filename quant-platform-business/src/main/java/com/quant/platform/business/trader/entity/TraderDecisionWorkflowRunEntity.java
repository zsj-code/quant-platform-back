package com.quant.platform.business.trader.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易决策工作流单次运行（与 {@code workflowRunKey} 一一对应）。
 */
@Data
@TableName("trader_decision_workflow_run")
public class TraderDecisionWorkflowRunEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("workflow_run_key")
    private String workflowRunKey;

    @TableField("request_code_raw")
    private String requestCodeRaw;

    @TableField("sec_code")
    private String secCode;

    @TableField("source_channel")
    private String sourceChannel;

    @TableField("status")
    private String status;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("decision_json")
    private String decisionJson;

    @TableField("llm_summary_text")
    private String llmSummaryText;

    @TableField("error_message")
    private String errorMessage;
}
