package com.tech.n.ai.api.news.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * News API 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class NewsExceptionHandler {
    
    /**
     * NewsNotFoundException 처리
     */
    @ExceptionHandler(NewsNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNewsNotFoundException(
            NewsNotFoundException e) {
        log.warn("News not found: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            "뉴스를 찾을 수 없습니다."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.NOT_FOUND,
            messageCode
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }
    
    /**
     * NewsValidationException 처리
     */
    @ExceptionHandler(NewsValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleNewsValidationException(
            NewsValidationException e) {
        log.warn("News validation failed: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR,
            "유효성 검증에 실패했습니다."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.VALIDATION_ERROR,
            messageCode
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * NewsDuplicateException 처리
     */
    @ExceptionHandler(NewsDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleNewsDuplicateException(
            NewsDuplicateException e) {
        log.warn("News duplicate: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_CONFLICT,
            "이미 존재하는 뉴스입니다."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.CONFLICT,
            messageCode
        );
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }
}
