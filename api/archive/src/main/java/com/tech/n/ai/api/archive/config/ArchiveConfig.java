package com.tech.n.ai.api.archive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Archive API 설정
 */
@ConfigurationProperties(prefix = "archive")
@Data
public class ArchiveConfig {
    
    /**
     * 복구 설정
     */
    private Restore restore = new Restore();
    
    @Data
    public static class Restore {
        /**
         * 복구 가능 최대 기간 (일)
         */
        private Integer maxDays = 30;
    }
}
