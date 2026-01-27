package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.api.auth.config.OAuthProperties;
import com.tech.n.ai.api.auth.dto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component("GOOGLE")
@RequiredArgsConstructor
public class GoogleOAuthProvider implements OAuthProvider {

    private final OAuthProperties.GoogleOAuthProperties googleProperties;

    @Qualifier("googleOAuthContract")
    private final OAuthProviderContract googleOAuthApi;

    @Override
    public String generateAuthorizationUrl(String clientId, String redirectUri, String state) {
        return UriComponentsBuilder
            .fromUriString(googleProperties.getAuthorizationEndpoint())
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri != null ? redirectUri : googleProperties.getRedirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", googleProperties.getScope())
            .queryParam("state", state)
            .queryParam("access_type", "online")
            .build()
            .toUriString();
    }

    @Override
    public String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri) {
        return googleOAuthApi.exchangeAccessToken(
            code,
            clientId,
            clientSecret,
            redirectUri != null ? redirectUri : googleProperties.getRedirectUri()
        );
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo feignUserInfo =
            googleOAuthApi.getUserInfo(accessToken);
        
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
