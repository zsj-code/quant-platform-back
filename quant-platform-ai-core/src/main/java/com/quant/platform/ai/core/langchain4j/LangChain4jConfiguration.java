package com.quant.platform.ai.core.langchain4j;

import com.quant.platform.ai.core.langchain4j.memory.RedisChatMemoryProvider;
import com.quant.platform.ai.core.service.FundamentalFactorOrchestrationService;
import com.quant.platform.ai.core.service.SentimentFactorOrchestrationService;
import com.quant.platform.ai.core.service.TechnicalFactorOrchestrationService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(LangChain4jOpenAiProperties.class)
@ConditionalOnProperty(prefix = "quant.ai.langchain4j.openai", name = "api-key")
public class LangChain4jConfiguration {

    @Bean
    public ChatModel chatModel(LangChain4jOpenAiProperties p) {
        return OpenAiChatModel.builder()
                .apiKey(p.getApiKey())
                .baseUrl(p.getBaseUrl())
                .modelName(p.getModel())
                .temperature(p.getTemperature())
                .timeout(Duration.ofSeconds(p.getTimeout()))
                .returnThinking(p.isReturnThinking())
                .sendThinking(p.isSendThinking())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel(LangChain4jOpenAiProperties p) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(p.getApiKey())
                .baseUrl(p.getBaseUrl())
                .modelName(p.resolveStreamingModel())
                .temperature(p.getTemperature())
                .timeout(Duration.ofSeconds(p.getTimeout()))
                .returnThinking(p.isReturnThinking())
                .sendThinking(p.isSendThinking())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(StringRedisTemplate redis) {
        return new RedisChatMemoryProvider(redis, 40, Duration.ofDays(7));
    }

    @Bean
    public QuantAiTools quantAiTools(FundamentalFactorOrchestrationService fundamental,
                                     TechnicalFactorOrchestrationService technical,
                                     SentimentFactorOrchestrationService sentiment) {
        return new QuantAiTools(fundamental, technical, sentiment);
    }

    @Bean
    public QuantAiAssistant quantAiAssistant(ChatModel model,
                                             ChatMemoryProvider memoryProvider,
                                             QuantAiTools tools) {
        return AiServices.builder(QuantAiAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryProvider)
                .tools(tools)
                .build();
    }

    @Bean
    public QuantAiStreamingAssistant quantAiStreamingAssistant(StreamingChatModel streamingModel,
                                                               ChatMemoryProvider memoryProvider,
                                                               QuantAiTools tools) {
        return AiServices.builder(QuantAiStreamingAssistant.class)
                .streamingChatModel(streamingModel)
                .chatMemoryProvider(memoryProvider)
                .tools(tools)
                .build();
    }
}

