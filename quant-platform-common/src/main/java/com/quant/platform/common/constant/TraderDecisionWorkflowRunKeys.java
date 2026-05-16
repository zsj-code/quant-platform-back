package com.quant.platform.common.constant;

import com.quant.platform.common.util.CommonUtil;

/**
 * 单次「交易决策工作流」运行锚定：人类可读、可日志检索，区别于 Snowflake/UUID；与 {@code runId}、{@code workflowRunId} 对齐。
 */
public final class TraderDecisionWorkflowRunKeys {

    /** SSE {@code workflow} 事件与协调器步骤 JSON 中共用的字段名 */
    public static final String JSON_KEY_WORKFLOW_RUN_KEY = "workflowRunKey";

    /** 与顶层 {@code runId}、{@code TraderDecisionResult#workflowRunId} 同值 */
    public static final String JSON_KEY_RUN_ID = "runId";

    private TraderDecisionWorkflowRunKeys() {
    }

    /**
     * 单次执行锚定：{@code trader-decision:sec:{6位代码}:{epochMillis}}，无效代码则为 {@code trader-decision:invalid:...}。
     */
    public static String newRunKey(String requestCodeRaw) {
        String sec = CommonUtil.normalizeSixDigitCode(requestCodeRaw);
        long t = System.currentTimeMillis();
        if (sec == null || sec.isEmpty()) {
            return "trader-decision:invalid:" + Integer.toUnsignedString(requestCodeRaw.hashCode()) + ":" + t;
        }
        return "trader-decision:sec:" + sec + ":" + t;
    }
}
