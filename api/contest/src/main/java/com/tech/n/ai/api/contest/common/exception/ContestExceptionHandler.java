package com.tech.n.ai.api.contest.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Contest API 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class ContestExceptionHandler {
    
    /**
     * ContestNotFoundException 처리
     */
    @ExceptionHandler(ContestNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleContestNotFoundException(
            ContestNotFoundException e) {
        log.warn("Contest not found: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            "대회를 찾을 수 없습니다."
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
     * ContestValidationException 처리
     */
    @ExceptionHandler(ContestValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleContestValidationException(
            ContestValidationException e) {
        log.warn("Contest validation failed: {}", e.getMessage());
        
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
     * ContestDuplicateException 처리
     */
    @ExceptionHandler(ContestDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleContestDuplicateException(
            ContestDuplicateException e) {
        log.warn("Contest duplicate: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_CONFLICT,
            "이미 존재하는 대회입니다."
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
