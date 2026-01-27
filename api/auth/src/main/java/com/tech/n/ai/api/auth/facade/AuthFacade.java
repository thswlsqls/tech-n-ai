package com.tech.n.ai.api.auth.facade;

import com.tech.n.ai.api.auth.dto.*;
import com.tech.n.ai.api.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthFacade {
    
    private final AuthService authService;
    
    public AuthResponse signup(SignupRequest request) {
        return authService.signup(request);
    }
    
    public TokenResponse login(LoginRequest request) {
        return authService.login(request);
    }
    
    public void logout(String userId, String refreshToken) {
        authService.logout(userId, refreshToken);
    }
    
    public void withdraw(String userId, WithdrawRequest request) {
        authService.withdraw(userId, request);
    }
    
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }
    
    public void verifyEmail(String token) {
        authService.verifyEmail(token);
    }
    
    public void requestPasswordReset(ResetPasswordRequest request) {
        authService.requestPasswordReset(request);
    }
    
    public void confirmPasswordReset(ResetPasswordConfirmRequest request) {
        authService.confirmPasswordReset(request);
    }
    
    public String startOAuthLogin(String provider) {
        return authService.startOAuthLogin(provider);
    }
    
    public TokenResponse handleOAuthCallback(String provider, String code, String state) {
        return authService.handleOAuthCallback(provider, code, state);
    }
}
