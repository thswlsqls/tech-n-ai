package com.tech.n.ai.api.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 관련 설정 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "internal-api.ai-update")
public class AgentConfig {

    /**
     * 내부 API 키 (Agent 엔드포인트 인증용)
     */
    private String apiKey;
}
