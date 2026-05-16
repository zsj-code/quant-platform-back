package com.quant.platform.ai.core.factor.sentiment;

/**
 * 情绪面因子分组，与 {@code md/情绪面.md} 四个章节对应。
 */
public enum SentimentFactorGroup {
    /** 一、市场整体情绪（仓位中枢） */
    MARKET_WIDE,
    /** 二、资金风格与流向（配置方向） */
    STYLE_AND_FLOW,
    /** 三、个股情绪强度（信号可信度） */
    STOCK_SPECIFIC,
    /** 四、衍生品与暗面情绪 */
    DERIVATIVES_AND_SHADOW
}
