package com.quant.platform.ai.core.langchain4j;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LangChain4j OpenAI-compatible 配置。
 * <p>
 * 说明：DeepSeek 等 OpenAI 兼容厂商也可通过 baseUrl 接入；阻塞与流式可配置不同 V4 模型。
 */
@ConfigurationProperties(prefix = "quant.ai.langchain4j.openai")
public class LangChain4jOpenAiProperties {
    /**
     * OpenAI / OpenAI-compatible API key（建议通过环境变量注入，不要写死在仓库里）。
     */
    private String apiKey;

    /**
     * OpenAI-compatible baseUrl，例如：{@code https://api.openai.com/v1}、{@code https://api.deepseek.com}
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 非流式模型（{@link dev.langchain4j.model.chat.ChatModel}）：规划、阻塞对话等。
     * DeepSeek V4 示例：{@code deepseek-v4-pro}、{@code deepseek-v4-flash}。
     */
    private String model = "deepseek-v4-pro";

    /**
     * 流式模型（{@link dev.langchain4j.model.chat.StreamingChatModel}）：SSE 逐 token 输出。
     * 未配置时回退为 {@link #model}。
     */
    private String streamingModel;

    /**
     * 采样温度。
     */
    private double temperature = 0.2;

    private int timeout = 12000;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStreamingModel() {
        return streamingModel;
    }

    public void setStreamingModel(String streamingModel) {
        this.streamingModel = streamingModel;
    }

    /**
     * 流式调用实际使用的模型名。
     */
    public String resolveStreamingModel() {
        if (streamingModel != null && !streamingModel.isBlank()) {
            return streamingModel.trim();
        }
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }
}

