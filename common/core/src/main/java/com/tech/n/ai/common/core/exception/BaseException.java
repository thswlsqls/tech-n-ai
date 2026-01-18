package com.tech.n.ai.common.core.exception;

/**
 * 기본 예외 클래스
 * 모든 커스텀 예외의 부모 클래스
 */
public class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final String messageCode;
    
    public BaseException(String errorCode, String messageCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.messageCode = messageCode;
    }
    
    public BaseException(String errorCode, String messageCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.messageCode = messageCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getMessageCode() {
        return messageCode;
    }
}

