package com.tech.n.ai.api.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthConfig implements ApplicationListener<ApplicationReadyEvent> {

    private final OAuthProperties oauthProperties;

    @Bean
    public OAuthProperties.GoogleOAuthProperties googleOAuthProperties() {
        return oauthProperties.getGoogle();
    }

    @Bean
    public OAuthProperties.NaverOAuthProperties naverOAuthProperties() {
        return oauthProperties.getNaver();
    }

    @Bean
    public OAuthProperties.KakaoOAuthProperties kakaoOAuthProperties() {
        return oauthProperties.getKakao();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validateOAuthProperties();
    }

    @PostConstruct
    public void init() {
        log.info("OAuth configuration initialized");
    }

    private void validateOAuthProperties() {
        validateGoogleProperties();
        validateNaverProperties();
        validateKakaoProperties();
        log.info("OAuth properties validation completed successfully");
    }

    private void validateGoogleProperties() {
        OAuthProperties.GoogleOAuthProperties google = oauthProperties.getGoogle();
        if (google == null) {
            throw new IllegalStateException("OAuth Google properties are not configured");
        }

        validateRequired("Google", "authorizationEndpoint", google.getAuthorizationEndpoint());
        validateRequired("Google", "redirectUri", google.getRedirectUri());
        validateUrl("Google", "authorizationEndpoint", google.getAuthorizationEndpoint());
        validateUrl("Google", "redirectUri", google.getRedirectUri());

        if (!StringUtils.hasText(google.getScope())) {
            log.warn("Google OAuth scope is not set, using default: openid email profile");
        }
    }

    private void validateNaverProperties() {
        OAuthProperties.NaverOAuthProperties naver = oauthProperties.getNaver();
        if (naver == null) {
            throw new IllegalStateException("OAuth Naver properties are not configured");
        }

        validateRequired("Naver", "authorizationEndpoint", naver.getAuthorizationEndpoint());
        validateRequired("Naver", "redirectUri", naver.getRedirectUri());
        validateUrl("Naver", "authorizationEndpoint", naver.getAuthorizationEndpoint());
        validateUrl("Naver", "redirectUri", naver.getRedirectUri());
    }

    private void validateKakaoProperties() {
        OAuthProperties.KakaoOAuthProperties kakao = oauthProperties.getKakao();
        if (kakao == null) {
            throw new IllegalStateException("OAuth Kakao properties are not configured");
        }

        validateRequired("Kakao", "authorizationEndpoint", kakao.getAuthorizationEndpoint());
        validateRequired("Kakao", "redirectUri", kakao.getRedirectUri());
        validateUrl("Kakao", "authorizationEndpoint", kakao.getAuthorizationEndpoint());
        validateUrl("Kakao", "redirectUri", kakao.getRedirectUri());
    }

    private void validateRequired(String provider, String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                String.format("OAuth %s %s is required but not configured. Please set the environment variable.", provider, fieldName)
            );
        }
    }

    private void validateUrl(String provider, String fieldName, String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalStateException(
                String.format("OAuth %s %s must be a valid URL starting with http:// or https://. Current value: %s", provider, fieldName, url)
            );
        }
    }
}
