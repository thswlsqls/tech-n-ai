package com.tech.n.ai.api.auth.controller;

import com.tech.n.ai.api.auth.dto.WithdrawRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController 통합 테스트")
@Disabled("통합 테스트는 테스트용 데이터베이스 설정이 필요합니다. application-test.yml 파일 생성 후 활성화하세요.")
class AuthControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("인증되지 않은 사용자는 회원탈퇴 불가")
    void withdraw_인증되지_않은_사용자() throws Exception {
        // Given
        WithdrawRequest request = new WithdrawRequest(null, null);
        
        // When & Then
        mockMvc.perform(delete("/api/v1/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("인증된 사용자 회원탈퇴 - 비밀번호 확인 없음")
    void withdraw_인증된_사용자_탈퇴_비밀번호_확인_없음() throws Exception {
        // Given
        String accessToken = obtainAccessToken();
        WithdrawRequest request = new WithdrawRequest(null, "탈퇴 사유");
        
        // When & Then
        mockMvc.perform(delete("/api/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    @DisplayName("인증된 사용자 회원탈퇴 - 비밀번호 확인 포함")
    void withdraw_인증된_사용자_탈퇴_비밀번호_확인_포함() throws Exception {
        // Given
        String accessToken = obtainAccessToken();
        WithdrawRequest request = new WithdrawRequest("Password123!", "탈퇴 사유");
        
        // When & Then
        mockMvc.perform(delete("/api/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    /**
     * Access Token 획득 헬퍼 메서드
     * 실제 테스트 환경에서는 사용자 생성 후 로그인하여 토큰 획득
     */
    private String obtainAccessToken() {
        // TODO: 실제 테스트 환경에서 사용자 생성 및 로그인하여 토큰 획득
        // 현재는 테스트 구조만 작성, 실제 구현은 테스트 환경 설정 후 완성
        return "test-access-token";
    }
}
