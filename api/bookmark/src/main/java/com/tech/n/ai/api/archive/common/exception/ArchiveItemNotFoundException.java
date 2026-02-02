package com.tech.n.ai.api.archive.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 원본 아이템(ContestDocument/NewsArticleDocument)을 찾을 수 없을 때 발생하는 예외
 */
public class ArchiveItemNotFoundException extends BaseException {
    
    public ArchiveItemNotFoundException(String message) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message);
    }
    
    public ArchiveItemNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message, cause);
    }
}
