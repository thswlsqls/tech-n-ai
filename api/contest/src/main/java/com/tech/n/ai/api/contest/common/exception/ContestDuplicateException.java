package com.tech.n.ai.api.contest.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * Contest 중복 시 발생하는 예외
 */
public class ContestDuplicateException extends BaseException {
    
    public ContestDuplicateException(String message) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message);
    }
    
    public ContestDuplicateException(String message, Throwable cause) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message, cause);
    }
}
