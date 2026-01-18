package com.tech.n.ai.client.feign.domain.oauth.mock;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KakaoOAuthMock implements OAuthProviderContract {
    
    @Override
    public String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri) {
        log.info("KakaoOAuthMock.exchangeAccessToken: code={}, clientId={}", code, clientId);
        return "mock-kakao-access-token";
    }
    
    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        log.info("KakaoOAuthMock.getUserInfo: accessToken={}", accessToken);
        return OAuthUserInfo.builder()
            .providerUserId("mock-kakao-user-id")
            .email("mock-kakao@example.com")
            .username("Mock Kakao User")
            .build();
    }
}
