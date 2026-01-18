package com.tech.n.ai.api.news.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * News API 설정
 */
@ConfigurationProperties(prefix = "news.internal")
@Data
public class NewsConfig {
    
    /**
     * 내부 API 키 (Batch 모듈에서 사용)
     */
    private String apiKey;
}
