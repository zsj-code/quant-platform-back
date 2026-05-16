package com.quant.platform.ai.core.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j AI Service：量化投研助手（最小可跑版本）。
 */
public interface QuantAiAssistant {

    @SystemMessage("你是量化投研与交易辅助助手。\n"
            + "你可以调用工具获取：基本面评估、技术面评估、情绪面评估。\n"
            + "你的输出要求：\n"
            + "- 先给结论，再给依据\n"
            + "- 指标尽量引用工具返回的数据字段\n"
            + "- 不能编造不存在的数据；拿不到就说明缺失并给下一步建议\n")
    String chat(@MemoryId String sessionId, @UserMessage String message);
}

