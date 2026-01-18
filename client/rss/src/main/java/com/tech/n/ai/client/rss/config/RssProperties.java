package com.tech.n.ai.client.rss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "rss")
@Data
public class RssProperties {
    
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private Map<String, RssSourceConfig> sources = new HashMap<>();
    
    @Data
    public static class RssSourceConfig {
        private String feedUrl;
        private String feedFormat; // "RSS_2.0" or "ATOM_1.0"
        private String updateFrequency;
    }
}
