package com.tech.n.ai.api.archive.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BaseException;

/**
 * 아카이브를 찾을 수 없을 때 발생하는 예외
 */
public class ArchiveNotFoundException extends BaseException {
    
    public ArchiveNotFoundException(String message) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message);
    }
    
    public ArchiveNotFoundException(String message, Throwable cause) {
        super(ErrorCodeConstants.NOT_FOUND, ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, message, cause);
    }
}
