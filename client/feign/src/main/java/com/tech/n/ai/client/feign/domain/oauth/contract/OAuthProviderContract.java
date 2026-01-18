package com.tech.n.ai.client.feign.domain.oauth.contract;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo;

/**
 * OAuth Provider Contract 인터페이스
 * 
 * OAuth Provider별 공통 비즈니스 메서드 시그니처 정의
 */
public interface OAuthProviderContract {
    
    /**
     * Authorization Code로 Access Token 교환
     * 
     * @param code Authorization Code
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param redirectUri Redirect URI
     * @return Access Token
     */
    String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri);
    
    /**
     * Access Token으로 사용자 정보 조회
     * 
     * @param accessToken Access Token
     * @return 사용자 정보
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
