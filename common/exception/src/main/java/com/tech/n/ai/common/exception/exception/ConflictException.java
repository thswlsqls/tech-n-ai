package com.tech.n.ai.common.exception.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 충돌 시 발생하는 예외
 * HTTP 409, 비즈니스 코드 "4005"
 */
public class ConflictException extends BaseException {
    
    public ConflictException(String message) {
        super(
            ErrorCodeConstants.CONFLICT,
            ErrorCodeConstants.MESSAGE_CODE_CONFLICT,
            message
        );
    }
    
    public ConflictException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.CONFLICT,
            ErrorCodeConstants.MESSAGE_CODE_CONFLICT,
            message,
            cause
        );
    }
}
