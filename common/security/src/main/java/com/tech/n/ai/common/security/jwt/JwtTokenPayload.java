package com.tech.n.ai.common.security.jwt;

/**
 * JWT 토큰 페이로드
 * 
 * @param userId 사용자 ID
 * @param email 이메일
 * @param role 역할 (USER, ADMIN)
 */
public record JwtTokenPayload(
    String userId,
    String email,
    String role
) {
}

