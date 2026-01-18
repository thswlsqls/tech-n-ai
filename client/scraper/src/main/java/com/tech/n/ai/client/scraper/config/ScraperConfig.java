package com.tech.n.ai.client.scraper.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ScraperProperties.class)
public class ScraperConfig {

    @Bean
    public WebClient.Builder webClientBuilder(ScraperProperties properties) {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }
}
