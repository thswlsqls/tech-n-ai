package com.tech.n.ai.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 토큰 관리 제공자
 * 
 * 참고:
 * - JWT 공식 스펙 (RFC 7519): https://tools.ietf.org/html/rfc7519
 * - jjwt 라이브러리 공식 문서: https://github.com/jwtk/jjwt
 */
@Slf4j
@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long accessTokenValidityInMinutes;
    private final long refreshTokenValidityInDays;
    
    public JwtTokenProvider(
        @Value("${jwt.secret-key:default-secret-key-change-in-production-minimum-256-bits}") String secretKey,
        @Value("${jwt.access-token-validity-minutes:60}") long accessTokenValidityInMinutes,
        @Value("${jwt.refresh-token-validity-days:7}") long refreshTokenValidityInDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMinutes = accessTokenValidityInMinutes;
        this.refreshTokenValidityInDays = refreshTokenValidityInDays;
    }
    
    /**
     * Access Token 생성
     * 
     * @param payload 토큰 페이로드
     * @return Access Token
     */
    public String generateAccessToken(JwtTokenPayload payload) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + Duration.ofMinutes(accessTokenValidityInMinutes).toMillis());
        
        return Jwts.builder()
            .subject(payload.userId())
            .claim("email", payload.email())
            .claim("role", payload.role())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }
    
    /**
     * Refresh Token 생성
     * 
     * @param payload 토큰 페이로드
     * @return Refresh Token
     */
    public String generateRefreshToken(JwtTokenPayload payload) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + Duration.ofDays(refreshTokenValidityInDays).toMillis());
        
        return Jwts.builder()
            .subject(payload.userId())
            .claim("email", payload.email())
            .claim("role", payload.role())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }
    
    /**
     * 토큰에서 페이로드 추출
     * 
     * @param token JWT 토큰
     * @return 토큰 페이로드
     */
    public JwtTokenPayload getPayloadFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return new JwtTokenPayload(
            claims.getSubject(),
            claims.get("email", String.class),
            claims.get("role", String.class)
        );
    }
    
    /**
     * 토큰 검증
     * 
     * @param token JWT 토큰
     * @return 유효한 토큰이면 true
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Refresh Token 만료 일시 계산
     * 
     * @return 만료 일시 (LocalDateTime)
     */
    public java.time.LocalDateTime getRefreshTokenExpiresAt() {
        return java.time.LocalDateTime.now().plusDays(refreshTokenValidityInDays);
    }
}

