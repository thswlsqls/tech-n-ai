package com.tech.n.ai.common.exception.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 외부 API 호출 실패 시 발생하는 예외
 * HTTP 503, 비즈니스 코드 "5003"
 */
public class ExternalApiException extends BaseException {
    
    public ExternalApiException(String message) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message
        );
    }
    
    public ExternalApiException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message,
            cause
        );
    }
}

