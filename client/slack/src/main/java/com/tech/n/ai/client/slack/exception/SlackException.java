package com.tech.n.ai.client.slack.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * Slack 연동 실패 시 발생하는 예외
 * HTTP 503, 비즈니스 코드 "5003" (ExternalApiException과 동일)
 */
public class SlackException extends BaseException {
    
    public SlackException(String message) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message
        );
    }
    
    public SlackException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message,
            cause
        );
    }
}
