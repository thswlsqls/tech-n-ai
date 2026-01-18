package com.tech.n.ai.client.scraper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "scraper")
@Data
public class ScraperProperties {
    
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private String userAgent = "ShrimpTM-Demo/1.0 (+https://github.com/your-repo)";
    private Map<String, ScraperSourceConfig> sources = new HashMap<>();
    
    @Data
    public static class ScraperSourceConfig {
        private String baseUrl;
        private String dataFormat; // "HTML" or "GraphQL"
        private int minIntervalSeconds = 1;
        private boolean requiresSelenium = false;
    }
}
