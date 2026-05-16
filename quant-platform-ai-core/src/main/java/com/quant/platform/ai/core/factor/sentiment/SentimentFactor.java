package com.quant.platform.ai.core.factor.sentiment;

import com.quant.platform.ai.core.factor.technical.FactorResult;

/**
 * 情绪面单因子：输出复用技术面 {@link FactorResult}，以便与 Agent/JSON 层结构一致。
 */
public interface SentimentFactor {
    String factorKey();

    SentimentFactorGroup group();

    FactorResult evaluate(SentimentContext ctx);
}
