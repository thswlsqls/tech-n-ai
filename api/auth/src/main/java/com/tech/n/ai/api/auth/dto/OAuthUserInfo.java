package com.tech.n.ai.api.auth.dto;


import lombok.Builder;


/**
 * OAuth 사용자 정보 DTO
 */
@Builder
public record OAuthUserInfo(
    String providerUserId,
    String email,
    String username
) {

}
