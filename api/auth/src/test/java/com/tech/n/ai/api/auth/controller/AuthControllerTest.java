package com.tech.n.ai.api.auth.controller;

import com.tech.n.ai.api.auth.dto.*;
import com.tech.n.ai.api.auth.facade.AuthFacade;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 슬라이스 테스트
 *
 * Security 비활성화하여 순수 Controller 로직만 테스트.
 * 인증/인가 테스트는 별도 통합테스트에서 수행.
 */
@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.tech\\.n\\.ai\\.common\\.security\\..*"
    )
)
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthFacade authFacade;

    // ========== POST /signup ==========

    @Nested
    @DisplayName("POST /api/v1/auth/signup")
    class Signup {

        @Test
        @DisplayName("정상 회원가입 - 200 OK")
        void signup_성공() throws Exception {
            SignupRequest request = new SignupRequest("test@example.com", "testuser", "Password1!");
            AuthResponse response = new AuthResponse(1L, "test@example.com", "testuser", "회원가입이 완료되었습니다.");
            when(authFacade.signup(any(SignupRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("이메일 누락 - 400 Bad Request")
        void signup_이메일_누락() throws Exception {
            String body = """
                {"username": "testuser", "password": "Password1!"}
                """;

            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 이메일 형식 - 400 Bad Request")
        void signup_잘못된_이메일() throws Exception {
            SignupRequest request = new SignupRequest("not-an-email", "testuser", "Password1!");

            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호 8자 미만 - 400 Bad Request")
        void signup_짧은_비밀번호() throws Exception {
            SignupRequest request = new SignupRequest("test@example.com", "testuser", "Pass1!");

            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("사용자명 3자 미만 - 400 Bad Request")
        void signup_짧은_사용자명() throws Exception {
            SignupRequest request = new SignupRequest("test@example.com", "ab", "Password1!");

            mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /login ==========

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("정상 로그인 - 200 OK")
        void login_성공() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "Password1!");
            TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token", "Bearer", 3600L, 604800L);
            when(authFacade.login(any(LoginRequest.class))).thenReturn(tokenResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("이메일 누락 - 400 Bad Request")
        void login_이메일_누락() throws Exception {
            String body = """
                {"password": "Password1!"}
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /logout ==========

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("RefreshToken 누락 - 400 Bad Request")
        void logout_토큰_누락() throws Exception {
            String body = "{}";

            mockMvc.perform(post("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /refresh ==========

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("정상 토큰 갱신 - 200 OK")
        void refresh_성공() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
            TokenResponse tokenResponse = new TokenResponse("new-access", "new-refresh", "Bearer", 3600L, 604800L);
            when(authFacade.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
        }

        @Test
        @DisplayName("RefreshToken 누락 - 400 Bad Request")
        void refresh_토큰_누락() throws Exception {
            String body = "{}";

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== GET /verify-email ==========

    @Nested
    @DisplayName("GET /api/v1/auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("정상 이메일 인증 - 200 OK")
        void verifyEmail_성공() throws Exception {
            mockMvc.perform(get("/api/v1/auth/verify-email")
                    .param("token", "verification-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));

            verify(authFacade).verifyEmail("verification-token");
        }

        @Test
        @DisplayName("토큰 파라미터 누락 - 400 Bad Request")
        void verifyEmail_토큰_누락() throws Exception {
            mockMvc.perform(get("/api/v1/auth/verify-email"))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /reset-password ==========

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("정상 비밀번호 재설정 요청 - 200 OK")
        void resetPassword_성공() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("test@example.com");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }
    }

    // ========== POST /reset-password/confirm ==========

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password/confirm")
    class ConfirmPasswordReset {

        @Test
        @DisplayName("정상 비밀번호 재설정 확인 - 200 OK")
        void confirmPasswordReset_성공() throws Exception {
            ResetPasswordConfirmRequest request = new ResetPasswordConfirmRequest("token", "NewPassword1!");

            mockMvc.perform(post("/api/v1/auth/reset-password/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }
    }

    // ========== GET /oauth2/{provider} ==========

    @Nested
    @DisplayName("GET /api/v1/auth/oauth2/{provider}")
    class StartOAuth {

        @Test
        @DisplayName("정상 OAuth 시작 - 302 Redirect")
        void startOAuth_성공() throws Exception {
            when(authFacade.startOAuthLogin("google"))
                .thenReturn("https://accounts.google.com/o/oauth2/auth?state=abc");

            mockMvc.perform(get("/api/v1/auth/oauth2/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://accounts.google.com/o/oauth2/auth?state=abc"));
        }
    }

    // ========== GET /oauth2/{provider}/callback ==========

    @Nested
    @DisplayName("GET /api/v1/auth/oauth2/{provider}/callback")
    class OAuthCallback {

        @Test
        @DisplayName("정상 OAuth 콜백 - 200 OK with TokenResponse")
        void oauthCallback_성공() throws Exception {
            TokenResponse tokenResponse = new TokenResponse("access", "refresh", "Bearer", 3600L, 604800L);
            when(authFacade.handleOAuthCallback("google", "auth-code", "state-value"))
                .thenReturn(tokenResponse);

            mockMvc.perform(get("/api/v1/auth/oauth2/google/callback")
                    .param("code", "auth-code")
                    .param("state", "state-value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));
        }
    }
}
