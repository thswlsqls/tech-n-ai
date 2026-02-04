package com.tech.n.ai.api.agent.config;

import com.tech.n.ai.client.feign.domain.oauth.config.OAuthFeignConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackages = {
        "com.tech.n.ai.api.agent",
        "com.tech.n.ai.domain.mongodb",
        "com.tech.n.ai.client.feign",
        "com.tech.n.ai.client.rss",
        "com.tech.n.ai.client.slack",
        "com.tech.n.ai.client.scraper",
        "com.tech.n.ai.common.core",
        "com.tech.n.ai.common.exception"
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = OAuthFeignConfig.class
    )
)
public class ServerConfig {
}
