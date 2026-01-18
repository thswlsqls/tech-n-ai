package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.api.auth.dto.OAuthUserInfo;

/**
 * OAuth Provider 공통 인터페이스
 */
public interface OAuthProvider {
    
    /**
     * OAuth 인증 URL 생성
     */
    String generateAuthorizationUrl(String clientId, String redirectUri, String state);
    
    /**
     * Authorization Code로 Access Token 교환
     */
    String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri);
    
    /**
     * Access Token으로 사용자 정보 조회
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
