package com.tech.n.ai.common.core.dto;

/**
 * 메시지 코드 객체
 * 국제화(i18n) 지원을 위한 메시지 코드
 * 
 * @param code 메시지 코드
 * @param text 메시지 텍스트 (기본 언어)
 */
public record MessageCode(
    String code,
    String text
) {
    /**
     * 성공 메시지 코드 생성
     */
    public static MessageCode success() {
        return new MessageCode("SUCCESS", "성공");
    }
}

