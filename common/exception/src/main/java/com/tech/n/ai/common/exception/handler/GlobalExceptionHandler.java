package com.tech.n.ai.common.exception.handler;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import com.tech.n.ai.common.core.exception.BaseException;
import com.tech.n.ai.common.core.exception.BusinessException;
import com.tech.n.ai.common.exception.exception.ExternalApiException;
import com.tech.n.ai.common.exception.exception.ForbiddenException;
import com.tech.n.ai.common.exception.exception.RateLimitExceededException;
import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.common.exception.logging.ExceptionContext;
import com.tech.n.ai.common.exception.logging.ExceptionLoggingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 모든 예외를 일관된 형식으로 처리
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    private final ExceptionLoggingService exceptionLoggingService;
    
    /**
     * BaseException 처리 (모든 커스텀 예외의 부모)
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e, HttpServletRequest request) {
        logException(e, request, determineSource(e));
        
        MessageCode messageCode = new MessageCode(e.getMessageCode(), getMessageText(e.getMessageCode()));
        ApiResponse<Void> response = ApiResponse.error(e.getErrorCode(), messageCode);
        
        HttpStatus httpStatus = mapErrorCodeToHttpStatus(e.getErrorCode());
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    /**
     * ResourceNotFoundException 처리
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
        ResourceNotFoundException e, HttpServletRequest request) {
        return handleBaseException(e, request);
    }
    
    /**
     * UnauthorizedException 처리
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
        UnauthorizedException e, HttpServletRequest request) {
        return handleBaseException(e, request);
    }
    
    /**
     * ForbiddenException 처리
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(
        ForbiddenException e, HttpServletRequest request) {
        return handleBaseException(e, request);
    }
    
    /**
     * RateLimitExceededException 처리
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(
        RateLimitExceededException e, HttpServletRequest request) {
        return handleBaseException(e, request);
    }
    
    /**
     * ExternalApiException 처리
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApiException(
        ExternalApiException e, HttpServletRequest request) {
        return handleBaseException(e, request);
    }
    
    
    /**
     * 유효성 검증 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException e, HttpServletRequest request) {
        logException(e, request, "READ");
        
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "유효성 검증 실패",
                (existing, replacement) -> existing
            ));
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR,
            "유효성 검증에 실패했습니다."
        );
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
            ErrorCodeConstants.VALIDATION_ERROR,
            messageCode,
            null,
            errors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error occurred", e);
        logException(e, request, "READ");
        
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_INTERNAL_SERVER_ERROR,
            "내부 서버 오류가 발생했습니다."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.INTERNAL_SERVER_ERROR,
            messageCode
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 예외 로깅
     */
    private void logException(Exception exception, HttpServletRequest request, String source) {
        try {
            ExceptionContext.ContextInfo context = buildContextInfo(request);
            
            if ("READ".equals(source)) {
                exceptionLoggingService.logReadException(exception, context);
            } else {
                exceptionLoggingService.logWriteException(exception, context);
            }
        } catch (Exception e) {
            log.error("Failed to log exception", e);
        }
    }
    
    /**
     * 컨텍스트 정보 생성
     */
    private ExceptionContext.ContextInfo buildContextInfo(HttpServletRequest request) {
        String module = request.getRequestURI().split("/")[1]; // 첫 번째 경로를 모듈명으로 사용
        String method = request.getMethod();
        Map<String, Object> parameters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length == 1) {
                parameters.put(key, values[0]);
            } else {
                parameters.put(key, values);
            }
        });
        
        String userId = request.getHeader("X-User-Id");
        String requestId = request.getHeader("X-Request-Id");
        
        return new ExceptionContext.ContextInfo(
            module,
            method,
            parameters,
            userId,
            requestId
        );
    }
    
    /**
     * 예외 소스 결정 (READ 또는 WRITE)
     */
    private String determineSource(BaseException e) {
        // ExternalApiException은 WRITE로 간주 (외부 API 호출)
        if (e instanceof ExternalApiException) {
            return "WRITE";
        }
        // 기본적으로 READ로 간주
        return "READ";
    }
    
    /**
     * 에러 코드를 HTTP 상태 코드로 매핑
     */
    private HttpStatus mapErrorCodeToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case ErrorCodeConstants.BAD_REQUEST, ErrorCodeConstants.VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case ErrorCodeConstants.AUTH_FAILED, ErrorCodeConstants.AUTH_REQUIRED -> HttpStatus.UNAUTHORIZED;
            case ErrorCodeConstants.FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ErrorCodeConstants.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCodeConstants.CONFLICT -> HttpStatus.CONFLICT;
            case ErrorCodeConstants.RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case ErrorCodeConstants.SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * 메시지 코드에 따른 메시지 텍스트 반환
     */
    private String getMessageText(String messageCode) {
        return switch (messageCode) {
            case ErrorCodeConstants.MESSAGE_CODE_SUCCESS -> "성공";
            case ErrorCodeConstants.MESSAGE_CODE_BAD_REQUEST -> "잘못된 요청입니다.";
            case ErrorCodeConstants.MESSAGE_CODE_AUTH_FAILED -> "인증에 실패했습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_AUTH_REQUIRED -> "인증이 필요합니다.";
            case ErrorCodeConstants.MESSAGE_CODE_FORBIDDEN -> "권한이 없습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND -> "리소스를 찾을 수 없습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_CONFLICT -> "충돌이 발생했습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_VALIDATION_ERROR -> "유효성 검증에 실패했습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_RATE_LIMIT_EXCEEDED -> "요청 한도를 초과했습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_INTERNAL_SERVER_ERROR -> "내부 서버 오류가 발생했습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_DATABASE_ERROR -> "데이터베이스 오류가 발생했습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_EXTERNAL_API_ERROR -> "외부 API 오류가 발생했습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE -> "서비스를 사용할 수 없습니다.";
            case ErrorCodeConstants.MESSAGE_CODE_TIMEOUT -> "요청 처리 시간이 초과되었습니다.";
            default -> "오류가 발생했습니다.";
        };
    }
}

