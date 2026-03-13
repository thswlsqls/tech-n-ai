package com.tech.n.ai.api.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Gateway 보안 경로 설정
 *
 * 공개/공개 제외/관리자 전용 경로를 YAML에서 관리합니다.
 * 경로 패턴은 Spring WebFlux의 PathPattern 형식을 따릅니다.
 *
 * @param publicPaths 인증 불필요 경로 패턴 목록
 * @param publicPathExclusions publicPaths에서 제외할 경로 패턴 (exclusion > inclusion)
 * @param adminOnlyPaths 관리자 전용 경로 패턴 목록 (ADMIN role 필수)
 */
@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(
    List<String> publicPaths,
    List<String> publicPathExclusions,
    List<String> adminOnlyPaths
) {
    public GatewaySecurityProperties {
        if (publicPaths == null) publicPaths = List.of();
        if (publicPathExclusions == null) publicPathExclusions = List.of();
        if (adminOnlyPaths == null) adminOnlyPaths = List.of();
    }
}
