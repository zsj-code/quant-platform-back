package com.quant.platform.ai.core.langchain4j;

import com.quant.platform.ai.core.service.FundamentalFactorOrchestrationService;
import com.quant.platform.ai.core.service.SentimentFactorOrchestrationService;
import com.quant.platform.ai.core.service.TechnicalFactorOrchestrationService;
import dev.langchain4j.agent.tool.Tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 提供给 LangChain4j Agent 调用的工具集合。
 * <p>
 * 注意：这里只做“读”操作，不做交易写入，避免副作用。
 */
public class QuantAiTools {
    private final FundamentalFactorOrchestrationService fundamental;
    private final TechnicalFactorOrchestrationService technical;
    private final SentimentFactorOrchestrationService sentiment;

    public QuantAiTools(FundamentalFactorOrchestrationService fundamental,
                        TechnicalFactorOrchestrationService technical,
                        SentimentFactorOrchestrationService sentiment) {
        this.fundamental = fundamental;
        this.technical = technical;
        this.sentiment = sentiment;
    }

    @Tool("对指定股票做基本面因子评估，返回分组结果与加载的财报计数")
    public Map<String, Object> fundamentalEvaluate(String symbol) {
        return fundamental.evaluate(symbol);
    }

    @Tool("对指定股票做技术面因子评估。minuteLimit 为分钟K线条数上限（例如2000）")
    public Map<String, Object> technicalEvaluate(String symbol, int minuteLimit) {
        return technical.evaluate(symbol, minuteLimit);
    }

    @Tool("对指定股票做情绪面因子评估）")
    public Map<String, Object> sentimentEvaluate(String symbol) {
        return sentiment.evaluate(symbol);
    }

}

