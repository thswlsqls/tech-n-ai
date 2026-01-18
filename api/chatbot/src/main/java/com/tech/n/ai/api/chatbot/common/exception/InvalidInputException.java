package com.tech.n.ai.api.chatbot.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BusinessException;

/**
 * 잘못된 입력 예외
 */
public class InvalidInputException extends BusinessException {
    
    public InvalidInputException(String message) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message);
    }
}
