package com.tech.n.ai.common.exception.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 권한 없음 시 발생하는 예외
 * HTTP 403, 비즈니스 코드 "4003"
 */
public class ForbiddenException extends BaseException {
    
    public ForbiddenException(String message) {
        super(
            ErrorCodeConstants.FORBIDDEN,
            ErrorCodeConstants.MESSAGE_CODE_FORBIDDEN,
            message
        );
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.FORBIDDEN,
            ErrorCodeConstants.MESSAGE_CODE_FORBIDDEN,
            message,
            cause
        );
    }
}

