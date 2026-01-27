package com.tech.n.ai.api.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Web 검색 설정
 */
@Configuration
public class WebSearchConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
