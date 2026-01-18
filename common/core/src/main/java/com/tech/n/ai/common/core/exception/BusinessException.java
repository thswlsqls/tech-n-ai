package com.tech.n.ai.common.core.exception;

/**
 * 비즈니스 예외
 * 비즈니스 로직 위반 시 사용
 */
public class BusinessException extends BaseException {
    
    public BusinessException(String errorCode, String messageCode, String message) {
        super(errorCode, messageCode, message);
    }
    
    public BusinessException(String errorCode, String messageCode, String message, Throwable cause) {
        super(errorCode, messageCode, message, cause);
    }
}

