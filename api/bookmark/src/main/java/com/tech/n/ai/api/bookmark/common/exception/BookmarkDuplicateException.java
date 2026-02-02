package com.tech.n.ai.api.bookmark.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 중복 북마크 저장 시도 시 발생하는 예외
 */
public class BookmarkDuplicateException extends BaseException {
    
    public BookmarkDuplicateException(String message) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message);
    }
    
    public BookmarkDuplicateException(String message, Throwable cause) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message, cause);
    }
}
