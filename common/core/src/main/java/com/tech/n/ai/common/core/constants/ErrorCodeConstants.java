package com.tech.n.ai.common.core.constants;

/**
 * 에러 코드 상수 정의
 * 
 * 에러 코드 체계:
 * - 2xxx: 성공
 * - 4xxx: 클라이언트 에러
 * - 5xxx: 서버 에러
 */
public final class ErrorCodeConstants {
    
    private ErrorCodeConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 성공 코드
     */
    public static final String SUCCESS = "2000";
    
    /**
     * 클라이언트 에러 코드
     */
    public static final String BAD_REQUEST = "4000";
    public static final String AUTH_FAILED = "4001";
    public static final String AUTH_REQUIRED = "4002";
    public static final String FORBIDDEN = "4003";
    public static final String NOT_FOUND = "4004";
    public static final String CONFLICT = "4005";
    public static final String VALIDATION_ERROR = "4006";
    public static final String RATE_LIMIT_EXCEEDED = "4029";
    
    /**
     * 서버 에러 코드
     */
    public static final String INTERNAL_SERVER_ERROR = "5000";
    public static final String DATABASE_ERROR = "5001";
    public static final String EXTERNAL_API_ERROR = "5002";
    public static final String SERVICE_UNAVAILABLE = "5003";
    public static final String TIMEOUT = "5004";
    
    /**
     * 메시지 코드 상수
     */
    public static final String MESSAGE_CODE_SUCCESS = "SUCCESS";
    public static final String MESSAGE_CODE_BAD_REQUEST = "BAD_REQUEST";
    public static final String MESSAGE_CODE_AUTH_FAILED = "AUTH_FAILED";
    public static final String MESSAGE_CODE_AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String MESSAGE_CODE_FORBIDDEN = "FORBIDDEN";
    public static final String MESSAGE_CODE_NOT_FOUND = "NOT_FOUND";
    public static final String MESSAGE_CODE_CONFLICT = "CONFLICT";
    public static final String MESSAGE_CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MESSAGE_CODE_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String MESSAGE_CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String MESSAGE_CODE_DATABASE_ERROR = "DATABASE_ERROR";
    public static final String MESSAGE_CODE_EXTERNAL_API_ERROR = "EXTERNAL_API_ERROR";
    public static final String MESSAGE_CODE_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String MESSAGE_CODE_TIMEOUT = "TIMEOUT";
}

