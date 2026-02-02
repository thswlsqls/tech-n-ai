package com.tech.n.ai.common.security.jwt;

/**
 * JWT 토큰 페이로드
 */
public record JwtTokenPayload(
    String userId,
    String email,
    String role
) {
}
