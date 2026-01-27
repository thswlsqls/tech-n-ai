package com.tech.n.ai.api.agent.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.agent",
    "com.tech.n.ai.client.feign",
    "com.tech.n.ai.client.slack",
    "com.tech.n.ai.client.scraper",
    "com.tech.n.ai.common.core",
    "com.tech.n.ai.common.exception"
})
public class ServerConfig {
}
