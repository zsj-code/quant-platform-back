package com.quant.platform.common.constant;

/**
 * 交易决策工作流入口标识（落库 {@code source_channel}）。当前仅流式路径写入 {@link #SSE_WORKFLOW}。
 */
public final class TraderDecisionWorkflowSourceChannel {

    public static final String SSE_WORKFLOW = "SSE_WORKFLOW";

    private TraderDecisionWorkflowSourceChannel() {
    }
}
