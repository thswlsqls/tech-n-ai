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
import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtTokenProvider(
        @Value("${jwt.secret-key:default-secret-key-change-in-production-minimum-256-bits}") String secretKey,
        @Value("${jwt.access-token-validity-minutes:60}") long accessTokenValidityInMinutes,
        @Value("${jwt.refresh-token-validity-days:7}") long refreshTokenValidityInDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = Duration.ofMinutes(accessTokenValidityInMinutes);
        this.refreshTokenValidity = Duration.ofDays(refreshTokenValidityInDays);
    }

    public String generateAccessToken(JwtTokenPayload payload) {
        return buildToken(payload, accessTokenValidity);
    }

    public String generateRefreshToken(JwtTokenPayload payload) {
        return buildToken(payload, refreshTokenValidity);
    }

    public JwtTokenPayload getPayloadFromToken(String token) {
        Claims claims = parseToken(token);
        return new JwtTokenPayload(
            claims.getSubject(),
            claims.get("email", String.class),
            claims.get("role", String.class)
        );
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public LocalDateTime getRefreshTokenExpiresAt() {
        return LocalDateTime.now().plusDays(refreshTokenValidity.toDays());
    }

    private String buildToken(JwtTokenPayload payload, Duration validity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity.toMillis());

        return Jwts.builder()
            .subject(payload.userId())
            .claim("email", payload.email())
            .claim("role", payload.role())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
