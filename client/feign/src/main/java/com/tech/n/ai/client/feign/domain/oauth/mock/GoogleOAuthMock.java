package com.tech.n.ai.client.feign.domain.oauth.mock;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleOAuthMock implements OAuthProviderContract {
    
    @Override
    public String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri) {
        log.info("GoogleOAuthMock.exchangeAccessToken: code={}, clientId={}", code, clientId);
        return "mock-google-access-token";
    }
    
    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        log.info("GoogleOAuthMock.getUserInfo: accessToken={}", accessToken);
        return OAuthUserInfo.builder()
            .providerUserId("mock-google-user-id")
            .email("mock-google@example.com")
            .username("Mock Google User")
            .build();
    }
}
