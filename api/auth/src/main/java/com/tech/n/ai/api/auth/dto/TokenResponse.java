package com.tech.n.ai.api.auth.dto;

/**
 * 토큰 응답 DTO
 */
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    Long refreshTokenExpiresIn
) {
}
