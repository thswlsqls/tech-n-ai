package com.tech.n.ai.api.contest.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * Contest 유효성 검증 실패 시 발생하는 예외
 */
public class ContestValidationException extends BaseException {
    
    public ContestValidationException(String message) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message);
    }
    
    public ContestValidationException(String message, Throwable cause) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message, cause);
    }
}
