package com.tech.n.ai.api.gateway.filter;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import com.tech.n.ai.common.security.jwt.JwtTokenPayload;
import com.tech.n.ai.common.security.jwt.JwtTokenProvider;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * JWT 인증 Gateway Filter
 *
 * 인증이 필요한 경로에 대해 JWT 토큰을 검증하고, 사용자 정보를 헤더에 주입합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGatewayFilter implements GatewayFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "x-user-id";
    private static final String USER_EMAIL_HEADER = "x-user-email";
    private static final String USER_ROLE_HEADER = "x-user-role";
    
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 인증 불필요 경로 확인
        String path = request.getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        
        // JWT 토큰 추출
        String token = extractToken(request);
        if (token == null) {
            log.debug("JWT token not found for path: {}", path);
            return handleUnauthorized(exchange);
        }
        
        // JWT 토큰 검증
        if (!jwtTokenProvider.validateToken(token)) {
            log.debug("Invalid JWT token for path: {}", path);
            return handleUnauthorized(exchange);
        }
        
        // 사용자 정보 추출 및 헤더 주입
        try {
            JwtTokenPayload payload = jwtTokenProvider.getPayloadFromToken(token);

            // 관리자 전용 경로 검증
            if (isAdminOnlyPath(path) && !"ADMIN".equals(payload.role())) {
                return handleForbidden(exchange);
            }

            ServerHttpRequest modifiedRequest = request.mutate()
                .header(USER_ID_HEADER, payload.userId())
                .header(USER_EMAIL_HEADER, payload.email())
                .header(USER_ROLE_HEADER, payload.role())
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .build();

            log.debug("JWT authentication successful for user: {}", payload.userId());
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.error("Error extracting JWT payload for path: {}", path, e);
            return handleUnauthorized(exchange);
        }
    }
    
    /**
     * 인증 불필요 경로 확인
     * 
     * @param path 요청 경로
     * @return 인증 불필요 경로이면 true
     */
    private boolean isPublicPath(String path) {
        // /api/v1/auth/admin/login은 공개 (관리자 로그인)
        if (path.equals("/api/v1/auth/admin/login")) {
            return true;
        }
        // /api/v1/auth/admin은 인증 필요
        if (path.startsWith("/api/v1/auth/admin")) {
            return false;
        }
        return path.startsWith("/api/v1/auth") ||
               path.startsWith("/api/v1/emerging-tech") ||
               path.startsWith("/actuator");
    }
    
    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * 
     * @param request ServerHttpRequest
     * @return JWT 토큰 (없으면 null)
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    
    /**
     * 인증 실패 시 401 Unauthorized 응답 반환
     * 
     * @param exchange ServerWebExchange
     * @return Mono<Void>
     */
    /**
     * 관리자 전용 경로 확인
     */
    private boolean isAdminOnlyPath(String path) {
        return path.startsWith("/api/v1/agent") ||
               path.startsWith("/api/v1/auth/admin");
    }

    /**
     * 권한 부족 시 403 Forbidden 응답 반환
     */
    private Mono<Void> handleForbidden(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_FORBIDDEN,
            "권한이 없습니다."
        );
        ApiResponse<Void> errorResponse = ApiResponse.error(
            ErrorCodeConstants.FORBIDDEN,
            messageCode
        );

        DataBufferFactory bufferFactory = response.bufferFactory();
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = bufferFactory.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error writing forbidden response", e);
            return response.setComplete();
        }
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        // ApiResponse 형식의 에러 응답 생성
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_AUTH_FAILED,
            "인증에 실패했습니다."
        );
        ApiResponse<Void> errorResponse = ApiResponse.error(
            ErrorCodeConstants.AUTH_FAILED,
            messageCode
        );
        
        // JSON 응답 작성 (Reactive 방식)
        DataBufferFactory bufferFactory = response.bufferFactory();
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = bufferFactory.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error writing unauthorized response", e);
            return response.setComplete();
        }
    }
}
