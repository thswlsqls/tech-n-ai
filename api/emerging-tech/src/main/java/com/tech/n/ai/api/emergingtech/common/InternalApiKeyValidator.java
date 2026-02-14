package com.tech.n.ai.api.emergingtech.common;

import com.tech.n.ai.api.emergingtech.config.EmergingTechConfig;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 내부 API 키 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalApiKeyValidator {

    private final EmergingTechConfig emergingTechConfig;

    /**
     * API 키 유효성 검증
     *
     * @throws UnauthorizedException 키가 유효하지 않을 경우
     */
    public void validate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("내부 API 키가 제공되지 않았습니다.");
        }

        if (emergingTechConfig.getApiKey() == null || emergingTechConfig.getApiKey().isBlank()) {
            log.warn("내부 API 키가 설정되지 않았습니다. 설정 파일을 확인하세요.");
            throw new UnauthorizedException("내부 API 키가 설정되지 않았습니다.");
        }

        if (!emergingTechConfig.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("유효하지 않은 내부 API 키입니다.");
        }
    }
}
