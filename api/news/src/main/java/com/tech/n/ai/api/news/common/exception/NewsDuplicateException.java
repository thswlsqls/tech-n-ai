package com.tech.n.ai.api.news.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * News 중복 시 발생하는 예외
 */
public class NewsDuplicateException extends BaseException {
    
    public NewsDuplicateException(String message) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message);
    }
    
    public NewsDuplicateException(String message, Throwable cause) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message, cause);
    }
}
