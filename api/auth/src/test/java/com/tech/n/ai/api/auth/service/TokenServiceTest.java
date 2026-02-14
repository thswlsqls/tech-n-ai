package com.tech.n.ai.api.auth.service;

import com.tech.n.ai.api.auth.dto.TokenResponse;
import com.tech.n.ai.common.security.jwt.JwtTokenPayload;
import com.tech.n.ai.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 단위 테스트")
class TokenServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private TokenService tokenService;

    @Nested
    @DisplayName("generateTokens")
    class GenerateTokens {

        @Test
        @DisplayName("정상 토큰 생성 - AccessToken, RefreshToken 포함")
        void generateTokens_성공() {
            // Given
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
            when(jwtTokenProvider.generateAccessToken(any(JwtTokenPayload.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(JwtTokenPayload.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getRefreshTokenExpiresAt()).thenReturn(expiresAt);

            // When
            TokenResponse result = tokenService.generateTokens(1L, "test@example.com", "USER");

            // Then
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(result.tokenType()).isEqualTo("Bearer");
            assertThat(result.expiresIn()).isEqualTo(3600L);
            assertThat(result.refreshTokenExpiresIn()).isEqualTo(604800L);
            verify(refreshTokenService).saveRefreshToken(1L, "refresh-token", expiresAt);
        }

        @Test
        @DisplayName("페이로드에 USER 역할이 설정되는지 확인")
        void generateTokens_페이로드_역할_확인() {
            // Given
            when(jwtTokenProvider.generateAccessToken(any(JwtTokenPayload.class))).thenReturn("access");
            when(jwtTokenProvider.generateRefreshToken(any(JwtTokenPayload.class))).thenReturn("refresh");
            when(jwtTokenProvider.getRefreshTokenExpiresAt()).thenReturn(LocalDateTime.now().plusDays(7));

            // When
            tokenService.generateTokens(1L, "test@example.com", "USER");

            // Then
            verify(jwtTokenProvider).generateAccessToken(argThat(payload ->
                "1".equals(payload.userId()) &&
                "test@example.com".equals(payload.email()) &&
                "USER".equals(payload.role())
            ));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰 - true 반환")
        void validateToken_유효() {
            when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);

            assertThat(tokenService.validateToken("valid-token")).isTrue();
        }

        @Test
        @DisplayName("유효하지 않은 토큰 - false 반환")
        void validateToken_무효() {
            when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

            assertThat(tokenService.validateToken("invalid-token")).isFalse();
        }
    }

    @Nested
    @DisplayName("getPayloadFromToken")
    class GetPayloadFromToken {

        @Test
        @DisplayName("토큰에서 페이로드 추출")
        void getPayloadFromToken_성공() {
            JwtTokenPayload expected = new JwtTokenPayload("1", "test@example.com", "USER");
            when(jwtTokenProvider.getPayloadFromToken("token")).thenReturn(expected);

            JwtTokenPayload result = tokenService.getPayloadFromToken("token");

            assertThat(result).isEqualTo(expected);
        }
    }
}
