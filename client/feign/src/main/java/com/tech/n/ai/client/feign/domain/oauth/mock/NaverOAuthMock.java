package com.tech.n.ai.client.feign.domain.oauth.mock;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NaverOAuthMock implements OAuthProviderContract {
    
    @Override
    public String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri) {
        log.info("NaverOAuthMock.exchangeAccessToken: code={}, clientId={}", code, clientId);
        return "mock-naver-access-token";
    }
    
    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        log.info("NaverOAuthMock.getUserInfo: accessToken={}", accessToken);
        return OAuthUserInfo.builder()
            .providerUserId("mock-naver-user-id")
            .email("mock-naver@example.com")
            .username("Mock Naver User")
            .build();
    }
}
