/**
 * 情绪面因子包：需求见项目根目录 {@code md/情绪面.md}；阈值常量见 {@link com.quant.platform.ai.core.factor.sentiment.SentimentMdThresholds}。
 * <p>
 * 四组因子由 {@link com.quant.platform.ai.core.factor.sentiment.SentimentFactorCatalog} 注册；
 * 各指标在输入序列未接入时返回 UNAVAILABLE，分档判定见各类中的 {@code classify} 静态方法（便于单测与后续接数）。
 */
package com.quant.platform.ai.core.factor.sentiment;
