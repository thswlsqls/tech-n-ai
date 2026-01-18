package com.tech.n.ai.api.contest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Contest API 설정
 */
@ConfigurationProperties(prefix = "contest.internal")
@Data
public class ContestConfig {
    
    /**
     * 내부 API 키 (Batch 모듈에서 사용)
     */
    private String apiKey;
}
