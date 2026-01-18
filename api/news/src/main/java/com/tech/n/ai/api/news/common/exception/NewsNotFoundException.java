package com.tech.n.ai.api.news.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * News를 찾을 수 없을 때 발생하는 예외
 */
public class NewsNotFoundException extends BaseException {
    
    public NewsNotFoundException(String message) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message);
    }
    
    public NewsNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message, cause);
    }
}
