package com.tech.n.ai.api.bookmark.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 북마크 유효성 검증 실패 시 발생하는 예외
 */
public class BookmarkValidationException extends BaseException {
    
    public BookmarkValidationException(String message) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message);
    }
    
    public BookmarkValidationException(String message, Throwable cause) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message, cause);
    }
}
