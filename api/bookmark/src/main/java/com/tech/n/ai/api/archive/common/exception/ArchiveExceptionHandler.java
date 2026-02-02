package com.tech.n.ai.api.archive.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Archive API 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class ArchiveExceptionHandler {
    
    /**
     * ArchiveNotFoundException 처리
     */
    @ExceptionHandler(ArchiveNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleArchiveNotFoundException(
            ArchiveNotFoundException e) {
        log.warn("Archive not found: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            "아카이브를 찾을 수 없습니다."
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
     * ArchiveDuplicateException 처리
     */
    @ExceptionHandler(ArchiveDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleArchiveDuplicateException(
            ArchiveDuplicateException e) {
        log.warn("Archive duplicate: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_CONFLICT,
            "이미 존재하는 아카이브입니다."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.CONFLICT,
            messageCode
        );
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }
    
    /**
     * ArchiveValidationException 처리
     */
    @ExceptionHandler(ArchiveValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleArchiveValidationException(
            ArchiveValidationException e) {
        log.warn("Archive validation failed: {}", e.getMessage());
        
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
     * ArchiveItemNotFoundException 처리
     */
    @ExceptionHandler(ArchiveItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleArchiveItemNotFoundException(
            ArchiveItemNotFoundException e) {
        log.warn("Archive item not found: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            "원본 아이템을 찾을 수 없습니다."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.NOT_FOUND,
            messageCode
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }
}
