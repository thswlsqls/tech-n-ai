package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.api.auth.config.OAuthProperties;
import com.tech.n.ai.api.auth.dto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleOAuthProvider 단위 테스트")
class GoogleOAuthProviderTest {

    @Mock
    private OAuthProperties.GoogleOAuthProperties googleProperties;

    @Mock
    private OAuthProviderContract googleOAuthApi;

    @InjectMocks
    private GoogleOAuthProvider googleOAuthProvider;

    @Nested
    @DisplayName("generateAuthorizationUrl")
    class GenerateAuthorizationUrl {

        @Test
        @DisplayName("정상 URL 생성 - 모든 파라미터 포함")
        void generateAuthorizationUrl_성공() {
            when(googleProperties.getAuthorizationEndpoint())
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
            when(googleProperties.getScope()).thenReturn("openid email profile");

            String url = googleOAuthProvider.generateAuthorizationUrl("client-id", "http://callback", "state-abc");

            assertThat(url).contains("client_id=client-id");
            assertThat(url).contains("redirect_uri=http://callback");
            assertThat(url).contains("response_type=code");
            assertThat(url).contains("state=state-abc");
            assertThat(url).contains("scope=openid email profile");
            assertThat(url).contains("access_type=online");
        }

        @Test
        @DisplayName("redirectUri null일 때 기본값 사용")
        void generateAuthorizationUrl_redirectUri_null() {
            when(googleProperties.getAuthorizationEndpoint())
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
            when(googleProperties.getRedirectUri()).thenReturn("http://default-callback");
            when(googleProperties.getScope()).thenReturn("openid email profile");

            String url = googleOAuthProvider.generateAuthorizationUrl("client-id", null, "state-abc");

            assertThat(url).contains("redirect_uri=http://default-callback");
        }
    }

    @Nested
    @DisplayName("exchangeAccessToken")
    class ExchangeAccessToken {

        @Test
        @DisplayName("정상 토큰 교환")
        void exchangeAccessToken_성공() {
            when(googleOAuthApi.exchangeAccessToken("auth-code", "cid", "csecret", "http://callback"))
                .thenReturn("access-token-123");

            String token = googleOAuthProvider.exchangeAccessToken("auth-code", "cid", "csecret", "http://callback");

            assertThat(token).isEqualTo("access-token-123");
        }

        @Test
        @DisplayName("redirectUri null일 때 기본값 사용")
        void exchangeAccessToken_redirectUri_null() {
            when(googleProperties.getRedirectUri()).thenReturn("http://default-callback");
            when(googleOAuthApi.exchangeAccessToken("auth-code", "cid", "csecret", "http://default-callback"))
                .thenReturn("access-token");

            String token = googleOAuthProvider.exchangeAccessToken("auth-code", "cid", "csecret", null);

            assertThat(token).isEqualTo("access-token");
        }
    }

    @Nested
    @DisplayName("getUserInfo")
    class GetUserInfo {

        @Test
        @DisplayName("정상 사용자 정보 조회")
        void getUserInfo_성공() {
            OAuthDto.OAuthUserInfo feignUserInfo = new OAuthDto.OAuthUserInfo(
                "google-123", "user@gmail.com", "googleuser"
            );
            when(googleOAuthApi.getUserInfo("access-token")).thenReturn(feignUserInfo);

            OAuthUserInfo result = googleOAuthProvider.getUserInfo("access-token");

            assertThat(result).isNotNull();
            assertThat(result.providerUserId()).isEqualTo("google-123");
            assertThat(result.email()).isEqualTo("user@gmail.com");
            assertThat(result.username()).isEqualTo("googleuser");
        }

        @Test
        @DisplayName("사용자 정보 null - null 반환")
        void getUserInfo_null() {
            when(googleOAuthApi.getUserInfo("access-token")).thenReturn(null);

            OAuthUserInfo result = googleOAuthProvider.getUserInfo("access-token");

            assertThat(result).isNull();
        }
    }
}
