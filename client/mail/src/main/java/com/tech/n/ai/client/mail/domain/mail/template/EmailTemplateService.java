package com.tech.n.ai.client.mail.domain.mail.template;

/**
 * 이메일 템플릿 렌더링을 추상화하는 인터페이스.
 */
public interface EmailTemplateService {
    
    /**
     * 회원가입 인증 이메일 HTML을 렌더링합니다.
     *
     * @param email 수신자 이메일
     * @param token 인증 토큰
     * @param verifyUrl 인증 완료 URL
     * @return 렌더링된 HTML 문자열
     */
    String renderVerificationEmail(String email, String token, String verifyUrl);
    
    /**
     * 비밀번호 재설정 이메일 HTML을 렌더링합니다.
     *
     * @param email 수신자 이메일
     * @param token 재설정 토큰
     * @param resetUrl 비밀번호 재설정 URL
     * @return 렌더링된 HTML 문자열
     */
    String renderPasswordResetEmail(String email, String token, String resetUrl);
}
