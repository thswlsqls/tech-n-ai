package com.tech.n.ai.api.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecureTokenGenerator 단위 테스트")
class SecureTokenGeneratorTest {

    @Test
    @DisplayName("토큰이 null이 아니고 비어있지 않음")
    void generate_비어있지_않음() {
        String token = SecureTokenGenerator.generate();

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Base64 URL-safe 인코딩 형식")
    void generate_Base64_URL_safe() {
        String token = SecureTokenGenerator.generate();

        // Base64 URL-safe 디코딩 가능해야 함
        byte[] decoded = Base64.getUrlDecoder().decode(token);
        assertThat(decoded).hasSize(32); // 32바이트
    }

    @Test
    @DisplayName("패딩 없는 Base64 형식")
    void generate_패딩_없음() {
        String token = SecureTokenGenerator.generate();

        assertThat(token).doesNotContain("=");
    }

    @RepeatedTest(10)
    @DisplayName("매번 고유한 토큰 생성")
    void generate_고유성() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            tokens.add(SecureTokenGenerator.generate());
        }
        // 100개 토큰이 모두 고유해야 함
        assertThat(tokens).hasSize(100);
    }
}
