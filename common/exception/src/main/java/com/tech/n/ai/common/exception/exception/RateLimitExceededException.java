package com.tech.n.ai.common.exception.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * Rate limit 초과 시 발생하는 예외
 * HTTP 429, 비즈니스 코드 "4029"
 */
public class RateLimitExceededException extends BaseException {
    
    public RateLimitExceededException(String message) {
        super(
            ErrorCodeConstants.RATE_LIMIT_EXCEEDED,
            ErrorCodeConstants.MESSAGE_CODE_RATE_LIMIT_EXCEEDED,
            message
        );
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.RATE_LIMIT_EXCEEDED,
            ErrorCodeConstants.MESSAGE_CODE_RATE_LIMIT_EXCEEDED,
            message,
            cause
        );
    }
}

