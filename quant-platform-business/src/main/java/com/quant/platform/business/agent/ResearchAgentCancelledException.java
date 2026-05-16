package com.quant.platform.business.agent;

/**
 * 用户主动停止 Research Agent 运行（规划 / 工具 / 大模型）。
 */
public class ResearchAgentCancelledException extends RuntimeException {

    public ResearchAgentCancelledException(String message) {
        super(message);
    }
}
