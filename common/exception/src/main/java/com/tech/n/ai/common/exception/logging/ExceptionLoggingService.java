package com.tech.n.ai.common.exception.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * 예외 로깅 서비스
 * MongoDB Atlas에 예외 로그를 저장
 * 
 * 참고: Spring Data MongoDB 공식 문서
 * https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionLoggingService {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * MongoDB Atlas 읽기 예외 저장
     * 
     * @param exception 발생한 예외
     * @param context 컨텍스트 정보
     */
    @Async
    public void logReadException(Exception exception, ExceptionContext.ContextInfo context) {
        try {
            ExceptionContext exceptionContext = buildExceptionContext(
                "READ",
                exception,
                context,
                determineSeverity(exception)
            );
            saveToMongoDB(exceptionContext);
        } catch (Exception e) {
            log.error("Failed to log read exception to MongoDB, falling back to local log", e);
            logExceptionLocally("READ", exception, context);
        }
    }
    
    /**
     * Amazon Aurora MySQL 쓰기 예외 저장
     * 
     * @param exception 발생한 예외
     * @param context 컨텍스트 정보
     */
    @Async
    public void logWriteException(Exception exception, ExceptionContext.ContextInfo context) {
        try {
            ExceptionContext exceptionContext = buildExceptionContext(
                "WRITE",
                exception,
                context,
                determineSeverity(exception)
            );
            saveToMongoDB(exceptionContext);
        } catch (Exception e) {
            log.error("Failed to log write exception to MongoDB, falling back to local log", e);
            logExceptionLocally("WRITE", exception, context);
        }
    }
    
    /**
     * 예외 컨텍스트 생성
     */
    private ExceptionContext buildExceptionContext(
        String source,
        Exception exception,
        ExceptionContext.ContextInfo context,
        String severity
    ) {
        String stackTrace = getStackTrace(exception);
        
        return new ExceptionContext(
            source,
            exception.getClass().getName(),
            exception.getMessage(),
            stackTrace,
            context,
            Instant.now(),
            severity
        );
    }
    
    /**
     * MongoDB에 저장
     */
    private void saveToMongoDB(ExceptionContext exceptionContext) {
        mongoTemplate.save(exceptionContext, "exception_logs");
    }
    
    /**
     * 로컬 로그 파일에 기록 (MongoDB 저장 실패 시 대체)
     */
    private void logExceptionLocally(String source, Exception exception, ExceptionContext.ContextInfo context) {
        log.error(
            "Exception logged locally - Source: {}, Type: {}, Message: {}, Module: {}, Method: {}, UserId: {}, RequestId: {}",
            source,
            exception.getClass().getName(),
            exception.getMessage(),
            context != null ? context.module() : null,
            context != null ? context.method() : null,
            context != null ? context.userId() : null,
            context != null ? context.requestId() : null,
            exception
        );
    }
    
    /**
     * 스택 트레이스 문자열로 변환
     */
    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 예외 심각도 결정
     */
    private String determineSeverity(Exception exception) {
        // 시스템 예외는 HIGH 또는 CRITICAL
        if (exception instanceof RuntimeException && 
            !(exception instanceof IllegalArgumentException || 
              exception instanceof IllegalStateException)) {
            return "HIGH";
        }
        
        // 일반적인 비즈니스 예외는 MEDIUM
        if (exception instanceof IllegalArgumentException || 
            exception instanceof IllegalStateException) {
            return "MEDIUM";
        }
        
        // 기타 예외는 MEDIUM
        return "MEDIUM";
    }
}

