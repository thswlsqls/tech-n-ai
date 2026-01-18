package com.tech.n.ai.api.archive.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 중복 아카이브 저장 시도 시 발생하는 예외
 */
public class ArchiveDuplicateException extends BaseException {
    
    public ArchiveDuplicateException(String message) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message);
    }
    
    public ArchiveDuplicateException(String message, Throwable cause) {
        super(ErrorCodeConstants.CONFLICT, ErrorCodeConstants.MESSAGE_CODE_CONFLICT, message, cause);
    }
}
