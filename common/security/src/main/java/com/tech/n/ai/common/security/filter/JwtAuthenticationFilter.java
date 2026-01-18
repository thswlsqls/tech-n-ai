package com.tech.n.ai.common.security.filter;


import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import com.tech.n.ai.common.security.jwt.JwtTokenProvider;
import com.tech.n.ai.common.security.jwt.JwtTokenPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;


/**
 * JWT 인증 필터
 * 
 * 참고: Spring Security 공식 문서
 * https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractTokenFromRequest(request);
        
        // 토큰이 없는 경우 필터 체인 계속 진행 (SecurityConfig의 authorizeHttpRequests가 처리)
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 토큰이 있는 경우 검증 수행
        try {
            if (jwtTokenProvider.validateToken(token)) {
                JwtTokenPayload payload = jwtTokenProvider.getPayloadFromToken(token);
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    payload.userId(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + payload.role()))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // SecurityContext 설정 (공식 문서 권장 방식: 빈 컨텍스트 생성 후 설정)
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } else {
                // 토큰이 있지만 유효하지 않은 경우 에러 반환
                log.warn("Invalid JWT token detected");
                handleAuthenticationError(response);
                return;
            }
        } catch (Exception e) {
            // 토큰 파싱 오류 등 예외 발생 시 에러 반환
            log.error("JWT authentication failed", e);
            handleAuthenticationError(response);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 요청에서 JWT 토큰 추출
     * 
     * @param request HttpServletRequest
     * @return JWT 토큰
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    
    /**
     * 인증 실패 시 에러 응답 처리
     * 
     * @param response HttpServletResponse
     * @throws IOException 응답 작성 오류
     */
    private void handleAuthenticationError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_AUTH_FAILED,
            "인증에 실패했습니다."
        );
        ApiResponse<Void> errorResponse = ApiResponse.error(
            ErrorCodeConstants.AUTH_FAILED,
            messageCode
        );
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

