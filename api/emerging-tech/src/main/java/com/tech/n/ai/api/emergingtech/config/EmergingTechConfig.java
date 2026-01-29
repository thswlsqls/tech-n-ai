package com.tech.n.ai.api.emergingtech.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Emerging Tech API 설정
 */
@ConfigurationProperties(prefix = "emerging-tech.internal")
@Data
public class EmergingTechConfig {

    /**
     * 내부 API 키 (Batch 모듈에서 사용)
     */
    private String apiKey;
}
