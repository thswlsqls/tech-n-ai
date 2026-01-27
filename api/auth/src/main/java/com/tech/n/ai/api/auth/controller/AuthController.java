package com.tech.n.ai.api.auth.controller;

import com.tech.n.ai.api.auth.dto.*;
import com.tech.n.ai.api.auth.facade.AuthFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthFacade authFacade;
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.signup(request)));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.login(request)));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            Authentication authentication) {
        authFacade.logout(authentication.getName(), request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @Valid @RequestBody(required = false) WithdrawRequest request,
            Authentication authentication) {
        authFacade.withdraw(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.refreshToken(request)));
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authFacade.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        authFacade.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody ResetPasswordConfirmRequest request) {
        authFacade.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @GetMapping("/oauth2/{provider}")
    public ResponseEntity<Void> startOAuthLogin(@PathVariable String provider) {
        String authUrl = authFacade.startOAuthLogin(provider);
        return ResponseEntity.status(302).header("Location", authUrl).build();
    }
    
    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<ApiResponse<TokenResponse>> handleOAuthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.handleOAuthCallback(provider, code, state)));
    }
}
