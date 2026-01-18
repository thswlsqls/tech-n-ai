package com.tech.n.ai.api.archive.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 아카이브 유효성 검증 실패 시 발생하는 예외
 */
public class ArchiveValidationException extends BaseException {
    
    public ArchiveValidationException(String message) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message);
    }
    
    public ArchiveValidationException(String message, Throwable cause) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message, cause);
    }
}
