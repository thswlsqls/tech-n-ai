package com.tech.n.ai.api.bookmark.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 북마크를 찾을 수 없을 때 발생하는 예외
 */
public class BookmarkNotFoundException extends BaseException {
    
    public BookmarkNotFoundException(String message) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message);
    }
    
    public BookmarkNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message, cause);
    }
}
