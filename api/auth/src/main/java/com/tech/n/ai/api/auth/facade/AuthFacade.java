package com.tech.n.ai.api.auth.facade;


import com.tech.n.ai.api.auth.dto.*;
import com.tech.n.ai.api.auth.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 인증 Facade
 * Controller와 Service 사이의 중간 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {
    
    private final AuthService authService;
    
    /**
     * 회원가입
     */
    public AuthResponse signup(SignupRequest request) {
        return authService.signup(request);
    }
    
    /**
     * 로그인
     */
    public TokenResponse login(LoginRequest request) {
        return authService.login(request);
    }
    
    /**
     * 로그아웃
     */
    public void logout(String userId, String refreshToken) {
        authService.logout(userId, refreshToken);
    }
    
    /**
     * 토큰 갱신
     */
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }
    
    /**
     * 이메일 인증
     */
    public void verifyEmail(String token) {
        authService.verifyEmail(token);
    }
    
    /**
     * 비밀번호 재설정 요청
     */
    public void requestPasswordReset(ResetPasswordRequest request) {
        authService.requestPasswordReset(request);
    }
    
    /**
     * 비밀번호 재설정 확인
     */
    public void confirmPasswordReset(ResetPasswordConfirmRequest request) {
        authService.confirmPasswordReset(request);
    }
    
    /**
     * OAuth 로그인 시작
     */
    public String startOAuthLogin(String provider) {
        return authService.startOAuthLogin(provider);
    }
    
    /**
     * OAuth 로그인 콜백
     */
    public TokenResponse handleOAuthCallback(String provider, String code, String state) {
        return authService.handleOAuthCallback(provider, code, state);
    }
}
