package com.tech.n.ai.common.core.util;

/**
 * 문자열 처리 유틸리티 클래스
 * 
 * 참고:
 * - Apache Commons Lang StringUtils 공식 문서
 *   https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html
 * - Java String API 공식 문서
 *   https://docs.oracle.com/javase/8/docs/api/java/lang/String.html
 */
public final class StringUtils {
    
    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * 문자열이 null이거나 빈 문자열인지 확인
     * 
     * @param str 확인할 문자열
     * @return null이거나 빈 문자열이면 true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    
    /**
     * 문자열이 null이거나 빈 문자열이거나 공백만 있는지 확인
     * 
     * @param str 확인할 문자열
     * @return null이거나 빈 문자열이거나 공백만 있으면 true
     */
    public static boolean isBlank(String str) {
        if (isEmpty(str)) {
            return true;
        }
        return str.trim().isEmpty();
    }
    
    /**
     * 문자열이 null이 아니고 빈 문자열이 아닌지 확인
     * 
     * @param str 확인할 문자열
     * @return null이 아니고 빈 문자열이 아니면 true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 문자열이 null이 아니고 빈 문자열이 아니고 공백만 있지 않은지 확인
     * 
     * @param str 확인할 문자열
     * @return null이 아니고 빈 문자열이 아니고 공백만 있지 않으면 true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 문자열의 앞뒤 공백 제거
     * null인 경우 null 반환
     * 
     * @param str 처리할 문자열
     * @return 공백이 제거된 문자열
     */
    public static String trim(String str) {
        if (str == null) {
            return null;
        }
        return str.trim();
    }
    
    /**
     * 문자열의 앞뒤 공백 제거 후 빈 문자열이면 null 반환
     * 
     * @param str 처리할 문자열
     * @return 공백이 제거된 문자열 (빈 문자열이면 null)
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        if (isEmpty(trimmed)) {
            return null;
        }
        return trimmed;
    }
    
    /**
     * null인 경우 빈 문자열 반환
     * 
     * @param str 처리할 문자열
     * @return null이면 빈 문자열, 아니면 원본 문자열
     */
    public static String defaultString(String str) {
        return str == null ? "" : str;
    }
    
    /**
     * null인 경우 기본값 반환
     * 
     * @param str 처리할 문자열
     * @param defaultStr 기본값
     * @return null이면 기본값, 아니면 원본 문자열
     */
    public static String defaultString(String str, String defaultStr) {
        return str == null ? defaultStr : str;
    }
}

