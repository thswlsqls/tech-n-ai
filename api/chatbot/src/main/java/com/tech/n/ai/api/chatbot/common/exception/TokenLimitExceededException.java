package com.tech.n.ai.api.chatbot.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.exception.BusinessException;

/**
 * 토큰 제한 초과 예외
 */
public class TokenLimitExceededException extends BusinessException {
    
    public TokenLimitExceededException(String message) {
        super(ErrorCodeConstants.VALIDATION_ERROR, ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR, message);
    }
}
