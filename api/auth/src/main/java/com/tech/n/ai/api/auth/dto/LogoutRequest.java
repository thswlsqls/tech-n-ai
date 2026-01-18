package com.tech.n.ai.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청 DTO
 */
public record LogoutRequest(
    @NotBlank(message = "Refresh Token은 필수입니다.")
    String refreshToken
) {
}
