package com.tech.n.ai.api.bookmark.common.exception;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bookmark API 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class BookmarkExceptionHandler {
    
    /**
     * BookmarkNotFoundException 처리
     */
    @ExceptionHandler(BookmarkNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookmarkNotFoundException(
            BookmarkNotFoundException e) {
        log.warn("Bookmark not found: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND,
            "북마크를 찾을 수 없습니다."
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
     * BookmarkDuplicateException 처리
     */
    @ExceptionHandler(BookmarkDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookmarkDuplicateException(
            BookmarkDuplicateException e) {
        log.warn("Bookmark duplicate: {}", e.getMessage());
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_CONFLICT,
            "이미 존재하는 북마크입니다."
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
     * BookmarkValidationException 처리
     */
    @ExceptionHandler(BookmarkValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookmarkValidationException(
            BookmarkValidationException e) {
        log.warn("Bookmark validation failed: {}", e.getMessage());
        
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
     * BookmarkItemNotFoundException 처리
     */
    @ExceptionHandler(BookmarkItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookmarkItemNotFoundException(
            BookmarkItemNotFoundException e) {
        log.warn("Bookmark item not found: {}", e.getMessage());
        
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
