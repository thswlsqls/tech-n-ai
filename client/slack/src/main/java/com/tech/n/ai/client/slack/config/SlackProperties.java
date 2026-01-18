package com.tech.n.ai.client.slack.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
@Data
public class SlackProperties {
    
    private Webhook webhook = new Webhook();
    private Bot bot = new Bot();
    private String defaultChannel = "#general";
    private Notification notification = new Notification();
    private RateLimit rateLimit = new RateLimit();
    
    @Data
    public static class Webhook {
        private String url;
        private boolean enabled = true;
    }
    
    @Data
    public static class Bot {
        private String token;
        private boolean enabled = false;
    }
    
    @Data
    public static class Notification {
        private String level = "INFO"; // INFO, WARN, ERROR
    }
    
    @Data
    public static class RateLimit {
        private long minIntervalMs = 1000;
        private boolean enabled = true;
    }
}
