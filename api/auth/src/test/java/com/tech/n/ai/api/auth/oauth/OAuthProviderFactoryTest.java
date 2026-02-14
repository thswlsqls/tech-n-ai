package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("OAuthProviderFactory 단위 테스트")
class OAuthProviderFactoryTest {

    private OAuthProviderFactory factory;
    private OAuthProvider googleProvider;
    private OAuthProvider kakaoProvider;
    private OAuthProvider naverProvider;

    @BeforeEach
    void setUp() {
        googleProvider = mock(OAuthProvider.class);
        kakaoProvider = mock(OAuthProvider.class);
        naverProvider = mock(OAuthProvider.class);

        factory = new OAuthProviderFactory(Map.of(
            "GOOGLE", googleProvider,
            "KAKAO", kakaoProvider,
            "NAVER", naverProvider
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"google", "GOOGLE", "Google"})
    @DisplayName("대소문자 무관하게 Google Provider 조회")
    void getProvider_Google_대소문자(String providerName) {
        OAuthProvider result = factory.getProvider(providerName);
        assertThat(result).isEqualTo(googleProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"kakao", "KAKAO"})
    @DisplayName("Kakao Provider 조회")
    void getProvider_Kakao(String providerName) {
        OAuthProvider result = factory.getProvider(providerName);
        assertThat(result).isEqualTo(kakaoProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"naver", "NAVER"})
    @DisplayName("Naver Provider 조회")
    void getProvider_Naver(String providerName) {
        OAuthProvider result = factory.getProvider(providerName);
        assertThat(result).isEqualTo(naverProvider);
    }

    @Test
    @DisplayName("존재하지 않는 Provider - ResourceNotFoundException")
    void getProvider_미지원() {
        assertThatThrownBy(() -> factory.getProvider("github"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
