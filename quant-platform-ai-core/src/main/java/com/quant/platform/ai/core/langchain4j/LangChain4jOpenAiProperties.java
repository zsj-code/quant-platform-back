package com.quant.platform.ai.core.langchain4j;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LangChain4j OpenAI-compatible 配置。
 * <p>
 * 说明：DeepSeek 等 OpenAI 兼容厂商也可通过 baseUrl 接入。
 */
@ConfigurationProperties(prefix = "quant.ai.langchain4j.openai")
public class LangChain4jOpenAiProperties {
    /**
     * OpenAI / OpenAI-compatible API key（建议通过环境变量注入，不要写死在仓库里）。
     */
    private String apiKey;

    /**
     * OpenAI-compatible baseUrl，例如：{@code https://api.openai.com/v1}、{@code https://api.deepseek.com/v1}
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 模型名，例如：gpt-4.1-mini / deepseek-reasoner（取决于厂商）。
     */
    private String model = "gpt-4.1-mini";

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

