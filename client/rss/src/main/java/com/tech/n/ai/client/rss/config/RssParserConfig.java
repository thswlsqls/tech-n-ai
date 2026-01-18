package com.tech.n.ai.client.rss.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(RssProperties.class)
public class RssParserConfig {

    @Bean
    public WebClient.Builder webClientBuilder(RssProperties properties) {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "Shrimp-TM-Demo/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }
}
