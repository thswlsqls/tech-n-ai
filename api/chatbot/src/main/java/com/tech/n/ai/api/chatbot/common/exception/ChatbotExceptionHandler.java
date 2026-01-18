package com.tech.n.ai.api.chatbot.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 챗봇 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class ChatbotExceptionHandler {
    
    /**
     * InvalidInputException 처리
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidInput(InvalidInputException e) {
        log.warn("Invalid input: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR,
            e.getMessage()
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.VALIDATION_ERROR,
            messageCode
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * TokenLimitExceededException 처리
     */
    @ExceptionHandler(TokenLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenLimitExceeded(TokenLimitExceededException e) {
        log.warn("Token limit exceeded: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR,
            e.getMessage()
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.VALIDATION_ERROR,
            messageCode
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * UnauthorizedException 처리
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException e) {
        log.warn("Unauthorized access: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_FORBIDDEN,
            e.getMessage()
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.FORBIDDEN,
            messageCode
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * ConversationSessionNotFoundException 처리
     */
    @ExceptionHandler(ConversationSessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionNotFound(ConversationSessionNotFoundException e) {
        log.warn("Session not found: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            e.getMessage()
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.NOT_FOUND,
            messageCode
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unexpected error in chatbot", e);
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_INTERNAL_SERVER_ERROR,
            "서버 오류가 발생했습니다."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.INTERNAL_SERVER_ERROR,
            messageCode
        );
        
        return ResponseEntity.internalServerError().body(response);
    }
}
