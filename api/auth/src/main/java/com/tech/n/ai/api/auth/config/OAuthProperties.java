package com.tech.n.ai.api.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private GoogleOAuthProperties google = new GoogleOAuthProperties();
    private NaverOAuthProperties naver = new NaverOAuthProperties();
    private KakaoOAuthProperties kakao = new KakaoOAuthProperties();

    @Getter
    @Setter
    public static class GoogleOAuthProperties {
        private String authorizationEndpoint;
        private String redirectUri;
        private String scope = "openid email profile";
    }

    @Getter
    @Setter
    public static class NaverOAuthProperties {
        private String authorizationEndpoint;
        private String redirectUri;
    }

    @Getter
    @Setter
    public static class KakaoOAuthProperties {
        private String authorizationEndpoint;
        private String redirectUri;
    }
}
