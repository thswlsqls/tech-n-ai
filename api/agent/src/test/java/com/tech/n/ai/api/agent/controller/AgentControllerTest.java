package com.tech.n.ai.api.agent.controller;

import com.tech.n.ai.api.agent.agent.AgentExecutionResult;
import com.tech.n.ai.api.agent.dto.request.AgentRunRequest;
import com.tech.n.ai.api.agent.facade.AgentFacade;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentController 단위 테스트")
class AgentControllerTest {

    @Mock
    private AgentFacade agentFacade;

    @InjectMocks
    private AgentController agentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/agent";
    private static final String TEST_USER_ID = "admin123";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders
                .standaloneSetup(agentController)
                .build();
    }

    // ========== POST /api/v1/agent/run 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/agent/run")
    class RunAgent {

        @Test
        @DisplayName("정상 실행 - 200 OK")
        void runAgent_정상실행() throws Exception {
            // Given
            AgentRunRequest request = new AgentRunRequest("OpenAI 최신 업데이트 확인", "session-123");
            AgentExecutionResult result = AgentExecutionResult.success(
                    "실행 완료: 3건의 업데이트 발견", 5, 2, 1500L);

            when(agentFacade.runAgent(eq(TEST_USER_ID), any(AgentRunRequest.class)))
                    .thenReturn(result);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("x-user-id", TEST_USER_ID)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("2000"))
                    .andExpect(jsonPath("$.data.success").value(true))
                    .andExpect(jsonPath("$.data.summary").value("실행 완료: 3건의 업데이트 발견"))
                    .andExpect(jsonPath("$.data.toolCallCount").value(5))
                    .andExpect(jsonPath("$.data.analyticsCallCount").value(2));
        }

        @Test
        @DisplayName("sessionId 선택적 - 없어도 성공")
        void runAgent_sessionId없음() throws Exception {
            // Given
            String requestBody = """
                    {"goal": "목표만 있는 요청"}
                    """;
            AgentExecutionResult result = AgentExecutionResult.success("완료", 1, 0, 100L);

            when(agentFacade.runAgent(eq(TEST_USER_ID), any(AgentRunRequest.class)))
                    .thenReturn(result);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("x-user-id", TEST_USER_ID)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("2000"));
        }

        @Test
        @DisplayName("x-user-id 헤더 전달 검증")
        void runAgent_userId헤더전달() throws Exception {
            // Given
            String customUserId = "custom-user-456";
            AgentRunRequest request = new AgentRunRequest("목표", null);
            AgentExecutionResult result = AgentExecutionResult.success("완료", 0, 0, 0);

            when(agentFacade.runAgent(eq(customUserId), any(AgentRunRequest.class)))
                    .thenReturn(result);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("x-user-id", customUserId)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(agentFacade).runAgent(eq(customUserId), any(AgentRunRequest.class));
        }

        @Test
        @DisplayName("응답에 AgentExecutionResult 포함")
        void runAgent_응답구조() throws Exception {
            // Given
            AgentRunRequest request = new AgentRunRequest("목표", "session");
            AgentExecutionResult result = new AgentExecutionResult(
                    true, "요약", 10, 3, 2500L, List.of());

            when(agentFacade.runAgent(any(), any())).thenReturn(result);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("x-user-id", TEST_USER_ID)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(jsonPath("$.data.success").value(true))
                    .andExpect(jsonPath("$.data.summary").value("요약"))
                    .andExpect(jsonPath("$.data.toolCallCount").value(10))
                    .andExpect(jsonPath("$.data.analyticsCallCount").value(3))
                    .andExpect(jsonPath("$.data.executionTimeMs").value(2500))
                    .andExpect(jsonPath("$.data.errors").isArray());
        }

        @Test
        @DisplayName("실행 실패 시 에러 정보 포함")
        void runAgent_실패응답() throws Exception {
            // Given
            AgentRunRequest request = new AgentRunRequest("목표", null);
            AgentExecutionResult failureResult = AgentExecutionResult.failure(
                    "실행 중 오류 발생", List.of("에러1", "에러2"));

            when(agentFacade.runAgent(any(), any())).thenReturn(failureResult);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("x-user-id", TEST_USER_ID)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(false))
                    .andExpect(jsonPath("$.data.errors").isArray())
                    .andExpect(jsonPath("$.data.errors[0]").value("에러1"))
                    .andExpect(jsonPath("$.data.errors[1]").value("에러2"));
        }

        @Test
        @DisplayName("x-user-id 헤더 누락 시 400 Bad Request")
        void runAgent_헤더누락() throws Exception {
            // Given
            AgentRunRequest request = new AgentRunRequest("목표", null);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 JSON 형식 - 400 Bad Request")
        void runAgent_잘못된JSON() throws Exception {
            // Given
            String invalidJson = "{ invalid json }";

            // When & Then
            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("x-user-id", TEST_USER_ID)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }
}
