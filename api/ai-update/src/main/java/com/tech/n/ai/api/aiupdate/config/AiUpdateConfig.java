package com.tech.n.ai.api.aiupdate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI Update API 설정
 */
@ConfigurationProperties(prefix = "ai-update.internal")
@Data
public class AiUpdateConfig {

    /**
     * 내부 API 키 (Batch 모듈에서 사용)
     */
    private String apiKey;
}
