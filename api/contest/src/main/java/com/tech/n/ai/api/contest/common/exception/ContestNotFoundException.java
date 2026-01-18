package com.tech.n.ai.api.contest.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * Contest를 찾을 수 없을 때 발생하는 예외
 */
public class ContestNotFoundException extends BaseException {
    
    public ContestNotFoundException(String message) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message);
    }
    
    public ContestNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message, cause);
    }
}
