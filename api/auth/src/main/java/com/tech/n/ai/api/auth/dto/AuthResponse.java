package com.tech.n.ai.api.auth.dto;

/**
 * 인증 응답 DTO
 */
public record AuthResponse(
    Long userId,
    String email,
    String username,
    String message
) {
}
