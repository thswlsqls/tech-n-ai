package com.tech.n.ai.api.bookmark.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bookmark API 설정
 */
@ConfigurationProperties(prefix = "bookmark")
@Data
public class BookmarkConfig {
    
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
