package com.tech.n.ai.common.exception.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 인증 실패 시 발생하는 예외
 * HTTP 401, 비즈니스 코드 "4001"
 */
public class UnauthorizedException extends BaseException {
    
    public UnauthorizedException(String message) {
        super(
            ErrorCodeConstants.AUTH_FAILED,
            ErrorCodeConstants.MESSAGE_CODE_AUTH_FAILED,
            message
        );
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.AUTH_FAILED,
            ErrorCodeConstants.MESSAGE_CODE_AUTH_FAILED,
            message,
            cause
        );
    }
}

