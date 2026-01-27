package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.api.auth.config.OAuthProperties;
import com.tech.n.ai.api.auth.dto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component("KAKAO")
@RequiredArgsConstructor
public class KakaoOAuthProvider implements OAuthProvider {

    private final OAuthProperties.KakaoOAuthProperties kakaoProperties;
    
    @Qualifier("kakaoOAuthContract")
    private final OAuthProviderContract kakaoOAuthApi;

    @Override
    public String generateAuthorizationUrl(String clientId, String redirectUri, String state) {
        return UriComponentsBuilder
            .fromUriString(kakaoProperties.getAuthorizationEndpoint())
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri != null ? redirectUri : kakaoProperties.getRedirectUri())
            .queryParam("response_type", "code")
            .queryParam("state", state)
            .build()
            .toUriString();
    }

    @Override
    public String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri) {
        return kakaoOAuthApi.exchangeAccessToken(
            code,
            clientId,
            clientSecret,
            redirectUri != null ? redirectUri : kakaoProperties.getRedirectUri()
        );
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo feignUserInfo =
            kakaoOAuthApi.getUserInfo(accessToken);
        
        if (feignUserInfo == null) {
            return null;
        }
        
        return OAuthUserInfo.builder()
            .providerUserId(feignUserInfo.providerUserId())
            .email(feignUserInfo.email())
            .username(feignUserInfo.username())
            .build();
    }
}
