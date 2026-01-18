package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuthProviderFactory {

    private final Map<String, OAuthProvider> oauthProviders;

    public OAuthProvider getProvider(String providerName) {
        String normalizedProviderName = providerName.toUpperCase();
        OAuthProvider provider = oauthProviders.get(normalizedProviderName);
        
        if (provider == null) {
            throw new ResourceNotFoundException("지원하지 않는 OAuth 제공자입니다: " + providerName);
        }
        
        return provider;
    }
}
