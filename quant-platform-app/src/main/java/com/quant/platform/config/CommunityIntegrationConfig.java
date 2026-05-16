package com.quant.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.client.TaogubaClient;
import com.quant.platform.common.props.CommunityPostSyncProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CommunityIntegrationConfig {

    @Bean
    @ConfigurationProperties(prefix = "quant.integration.community-post")
    public CommunityPostSyncProperties communityPostSyncProperties() {
        return new CommunityPostSyncProperties();
    }

    @Bean
    public TaogubaClient taogubaClient(@Qualifier("tgbWebClient") WebClient tgbWebClient, ObjectMapper objectMapper) {
        return new TaogubaClient(tgbWebClient, objectMapper);
    }
}
