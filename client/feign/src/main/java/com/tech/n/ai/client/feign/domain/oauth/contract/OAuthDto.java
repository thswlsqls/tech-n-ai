package com.tech.n.ai.client.feign.domain.oauth.contract;

import lombok.Builder;

/**
 * OAuth DTO 클래스
 */
public class OAuthDto {
    
    @Builder
    public record OAuthTokenRequest(
        String code,
        String clientId,
        String clientSecret,
        String redirectUri,
        String grantType
    ) {}
    
    @Builder
    public record OAuthTokenResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        String refreshToken
    ) {}
    
    @Builder
    public record OAuthUserInfo(
        String providerUserId,
        String email,
        String username
    ) {}
    
    // Google OAuth Response
    @Builder
    public record GoogleTokenResponse(
        String access_token,
        String token_type,
        Long expires_in,
        String refresh_token,
        String scope
    ) {}
    
    @Builder
    public record GoogleUserInfoResponse(
        String id,
        String email,
        Boolean verified_email,
        String name,
        String given_name,
        String family_name,
        String picture,
        String locale
    ) {}
    
    // Naver OAuth Response
    @Builder
    public record NaverTokenResponse(
        String access_token,
        String refresh_token,
        String token_type,
        Long expires_in,
        String error,
        String error_description
    ) {}
    
    @Builder
    public record NaverUserInfoResponse(
        String resultcode,
        String message,
        NaverUserInfo response
    ) {}
    
    @Builder
    public record NaverUserInfo(
        String id,
        String email,
        String name,
        String nickname,
        String profile_image,
        String age,
        String gender,
        String birthday,
        String birthyear
    ) {}
    
    // Kakao OAuth Response
    @Builder
    public record KakaoTokenResponse(
        String access_token,
        String token_type,
        Long expires_in,
        String refresh_token,
        String scope
    ) {}
    
    @Builder
    public record KakaoUserInfoResponse(
        Long id,
        KakaoAccount kakao_account,
        KakaoProperties properties
    ) {}
    
    @Builder
    public record KakaoAccount(
        String email,
        Boolean email_needs_agreement,
        Boolean is_email_valid,
        Boolean is_email_verified,
        KakaoProfile profile
    ) {}
    
    @Builder
    public record KakaoProfile(
        String nickname,
        String thumbnail_image_url,
        String profile_image_url
    ) {}
    
    @Builder
    public record KakaoProperties(
        String nickname
    ) {}
}
