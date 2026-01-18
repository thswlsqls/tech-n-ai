package com.tech.n.ai.common.core.util;

/**
 * 데이터 검증 유틸리티 클래스
 * 
 * 참고: Jakarta Bean Validation 공식 문서
 * https://beanvalidation.org/
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 이메일 형식 정규식
     * RFC 5322 기반 간단한 이메일 검증
     */
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    
    /**
     * 이메일 형식 검증
     * 
     * @param email 검증할 이메일
     * @return 유효한 이메일 형식이면 true
     */
    public static boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return email.matches(EMAIL_PATTERN);
    }
    
    /**
     * 입력값이 null인지 확인
     * 
     * @param value 확인할 값
     * @return null이면 true
     */
    public static boolean isNull(Object value) {
        return value == null;
    }
    
    /**
     * 입력값이 null이 아닌지 확인
     * 
     * @param value 확인할 값
     * @return null이 아니면 true
     */
    public static boolean isNotNull(Object value) {
        return !isNull(value);
    }
    
    /**
     * 문자열이 null이거나 빈 문자열인지 확인
     * 
     * @param str 확인할 문자열
     * @return null이거나 빈 문자열이면 true
     */
    public static boolean isEmpty(String str) {
        return StringUtils.isEmpty(str);
    }
    
    /**
     * 문자열이 null이 아니고 빈 문자열이 아닌지 확인
     * 
     * @param str 확인할 문자열
     * @return null이 아니고 빈 문자열이 아니면 true
     */
    public static boolean isNotEmpty(String str) {
        return StringUtils.isNotEmpty(str);
    }
    
    /**
     * 비밀번호 강도 검증
     * 최소 8자 이상, 영문자, 숫자, 특수문자 중 2가지 이상 포함
     * 
     * @param password 검증할 비밀번호
     * @return 유효한 비밀번호이면 true
     */
    public static boolean isValidPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return false;
        }
        
        if (password.length() < 8) {
            return false;
        }
        
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        
        int criteriaCount = 0;
        if (hasLetter) criteriaCount++;
        if (hasDigit) criteriaCount++;
        if (hasSpecial) criteriaCount++;
        
        return criteriaCount >= 2;
    }
}

