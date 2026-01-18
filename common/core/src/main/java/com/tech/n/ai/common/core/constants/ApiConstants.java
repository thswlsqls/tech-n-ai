package com.tech.n.ai.common.core.constants;

/**
 * API 관련 상수 정의
 */
public final class ApiConstants {
    
    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * API 버전
     */
    public static final String API_VERSION = "v1";
    
    /**
     * 엔드포인트 경로 상수
     */
    public static final String API_BASE_PATH = "/api/" + API_VERSION;
    
    /**
     * HTTP 헤더 상수
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_X_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_X_USER_ID = "X-User-Id";
    
    /**
     * Content-Type 값
     */
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";
}

