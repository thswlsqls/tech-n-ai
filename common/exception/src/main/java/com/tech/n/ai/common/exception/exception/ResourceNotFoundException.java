package com.tech.n.ai.common.exception.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 * HTTP 404, 비즈니스 코드 "4004"
 */
public class ResourceNotFoundException extends BaseException {
    
    public ResourceNotFoundException(String message) {
        super(
            ErrorCodeConstants.NOT_FOUND,
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            message
        );
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.NOT_FOUND,
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            message,
            cause
        );
    }
}

