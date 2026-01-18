package com.tech.n.ai.client.slack.config;

import com.tech.n.ai.client.slack.domain.slack.api.SlackApi;
import com.tech.n.ai.client.slack.domain.slack.client.SlackClient;
import com.tech.n.ai.client.slack.domain.slack.client.SlackWebhookClient;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import com.tech.n.ai.client.slack.domain.slack.service.SlackNotificationService;
import com.tech.n.ai.client.slack.domain.slack.service.SlackNotificationServiceImpl;
import com.tech.n.ai.client.slack.util.SlackRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SlackProperties.class)
public class SlackConfig {

    @Bean
    public WebClient.Builder webClientBuilder(SlackProperties properties) {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }
    
    @Bean
    @ConditionalOnProperty(name = "slack.webhook.enabled", havingValue = "true", matchIfMissing = true)
    public SlackClient slackWebhookClient(
            WebClient.Builder webClientBuilder,
            SlackProperties properties,
            SlackRateLimiter rateLimiter) {
        return new SlackWebhookClient(
            webClientBuilder.build(),
            properties,
            rateLimiter
        );
    }
    
    @Bean
    public SlackContract slackContract(SlackClient slackClient) {
        return new SlackApi(slackClient);
    }
    
    @Bean
    public SlackNotificationService slackNotificationService(SlackContract slackContract) {
        return new SlackNotificationServiceImpl(slackContract);
    }
}
