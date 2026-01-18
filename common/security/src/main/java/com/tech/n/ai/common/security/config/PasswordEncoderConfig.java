package com.tech.n.ai.common.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder 설정
 * 
 * 참고: Spring Security PasswordEncoder 공식 문서
 * https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
 */
@Configuration
public class PasswordEncoderConfig {
    
    /**
     * BCryptPasswordEncoder 빈 등록
     * salt rounds: 12
     * 
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

