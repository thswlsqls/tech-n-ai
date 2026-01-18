package com.tech.n.ai.api.auth.controller;


import com.tech.n.ai.api.auth.dto.*;
import com.tech.n.ai.api.auth.facade.AuthFacade;

import com.tech.n.ai.common.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


/**
 * 인증 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthFacade authFacade;
    
    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authFacade.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authFacade.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        authFacade.logout(userId, request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authFacade.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 이메일 인증
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authFacade.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 비밀번호 재설정 요청
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authFacade.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 비밀번호 재설정 확인
     */
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody ResetPasswordConfirmRequest request) {
        authFacade.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * OAuth 로그인 시작
     */
    @GetMapping("/oauth2/{provider}")
    public ResponseEntity<Void> startOAuthLogin(@PathVariable String provider) {
        String authUrl = authFacade.startOAuthLogin(provider);
        return ResponseEntity.status(302).header("Location", authUrl).build();
    }
    
    /**
     * OAuth 로그인 콜백
     */
    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<ApiResponse<TokenResponse>> handleOAuthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        TokenResponse response = authFacade.handleOAuthCallback(provider, code, state);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
