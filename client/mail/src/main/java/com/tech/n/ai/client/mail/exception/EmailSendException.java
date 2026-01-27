package com.tech.n.ai.client.mail.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 이메일 발송 실패 시 발생하는 예외.
 * HTTP 503, 비즈니스 코드 "5003"
 */
public class EmailSendException extends BaseException {
    
    public EmailSendException(String message) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message
        );
    }
    
    public EmailSendException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message,
            cause
        );
    }
}
