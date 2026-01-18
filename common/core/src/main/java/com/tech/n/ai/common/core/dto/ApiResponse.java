package com.tech.n.ai.common.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 표준 API 응답 래퍼
 * 
 * @param <T> 응답 데이터 타입
 * @param code 응답 코드 (성공: "2000", 기타 비즈니스 코드)
 * @param messageCode 메시지 코드 객체 (국제화 지원)
 * @param message 응답 메시지 (성공 응답에만 포함, 기본: "success")
 * @param data 응답 데이터 (제네릭 타입)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    String code,
    MessageCode messageCode,
    String message,
    T data
) {
    /**
     * 성공 응답 생성
     * 
     * @param data 응답 데이터
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            "2000",
            MessageCode.success(),
            "success",
            data
        );
    }
    
    /**
     * 성공 응답 생성 (데이터 없음)
     * 
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }
    
    /**
     * 에러 응답 생성
     * 
     * @param code 에러 코드
     * @param messageCode 메시지 코드
     * @return 에러 응답 (message와 data 필드는 null)
     */
    public static <T> ApiResponse<T> error(String code, MessageCode messageCode) {
        return new ApiResponse<>(
            code,
            messageCode,
            null,
            null
        );
    }
}

