package com.tech.n.ai.api.agent.facade;

import com.tech.n.ai.api.agent.agent.AgentExecutionResult;
import com.tech.n.ai.api.agent.agent.EmergingTechAgent;
import com.tech.n.ai.api.agent.dto.request.AgentRunRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AgentFacade 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentFacade 단위 테스트")
class AgentFacadeTest {

    @Mock
    private EmergingTechAgent agent;

    @InjectMocks
    private AgentFacade facade;

    // ========== runAgent 테스트 ==========

    @Nested
    @DisplayName("runAgent")
    class RunAgent {

        @Test
        @DisplayName("정상 실행 - sessionId 제공된 경우")
        void runAgent_sessionId제공() {
            // Given
            String userId = "admin123";
            String goal = "OpenAI 최신 업데이트 확인";
            String sessionId = "custom-session-id";
            AgentRunRequest request = new AgentRunRequest(goal, sessionId);

            AgentExecutionResult expectedResult = AgentExecutionResult.success("완료", 5, 2, 1000L);
            when(agent.execute(goal, sessionId)).thenReturn(expectedResult);

            // When
            AgentExecutionResult result = facade.runAgent(userId, request);

            // Then
            assertThat(result).isEqualTo(expectedResult);
            verify(agent).execute(goal, sessionId);
        }

        @Test
        @DisplayName("sessionId 자동 생성 - 요청에 없는 경우")
        void runAgent_sessionId_null() {
            // Given
            String userId = "admin123";
            String goal = "목표";
            AgentRunRequest request = new AgentRunRequest(goal, null);

            AgentExecutionResult expectedResult = AgentExecutionResult.success("완료", 3, 1, 500L);
            when(agent.execute(eq(goal), anyString())).thenReturn(expectedResult);

            // When
            AgentExecutionResult result = facade.runAgent(userId, request);

            // Then
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("sessionId 자동 생성 - 빈 문자열인 경우")
        void runAgent_sessionId_빈문자열() {
            // Given
            String userId = "admin123";
            String goal = "목표";
            AgentRunRequest request = new AgentRunRequest(goal, "");

            AgentExecutionResult expectedResult = AgentExecutionResult.success("완료", 1, 0, 100L);
            when(agent.execute(eq(goal), anyString())).thenReturn(expectedResult);

            // When
            AgentExecutionResult result = facade.runAgent(userId, request);

            // Then
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("sessionId 자동 생성 - 공백만 있는 경우")
        void runAgent_sessionId_공백() {
            // Given
            String userId = "user456";
            String goal = "목표";
            AgentRunRequest request = new AgentRunRequest(goal, "   ");

            AgentExecutionResult expectedResult = AgentExecutionResult.success("완료", 1, 0, 100L);
            when(agent.execute(eq(goal), anyString())).thenReturn(expectedResult);

            // When
            facade.runAgent(userId, request);

            // Then
            ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(agent).execute(eq(goal), sessionIdCaptor.capture());

            // 자동 생성된 sessionId 확인
            String capturedSessionId = sessionIdCaptor.getValue();
            assertThat(capturedSessionId).startsWith("admin-user456-");
        }

        @Test
        @DisplayName("sessionId 형식: admin-{userId}-{uuid8}")
        void runAgent_sessionId_형식() {
            // Given
            String userId = "testUser";
            String goal = "목표";
            AgentRunRequest request = new AgentRunRequest(goal, null);

            when(agent.execute(anyString(), anyString())).thenReturn(
                    AgentExecutionResult.success("완료", 0, 0, 0));

            // When
            facade.runAgent(userId, request);

            // Then
            ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(agent).execute(eq(goal), sessionIdCaptor.capture());

            String sessionId = sessionIdCaptor.getValue();
            assertThat(sessionId).startsWith("admin-testUser-");
            // UUID 8자리 형식 확인 (admin-testUser-xxxxxxxx)
            assertThat(sessionId).matches("admin-testUser-[a-f0-9]{8}");
        }

        @Test
        @DisplayName("agent.execute 호출 검증")
        void runAgent_execute호출검증() {
            // Given
            String userId = "admin";
            String goal = "Anthropic SDK 업데이트 확인";
            String sessionId = "session-123";
            AgentRunRequest request = new AgentRunRequest(goal, sessionId);

            when(agent.execute(goal, sessionId)).thenReturn(
                    AgentExecutionResult.success("실행 완료", 10, 3, 2000L));

            // When
            facade.runAgent(userId, request);

            // Then
            verify(agent).execute(goal, sessionId);
        }

        @Test
        @DisplayName("실패 결과 반환")
        void runAgent_실패결과() {
            // Given
            String userId = "admin";
            AgentRunRequest request = new AgentRunRequest("goal", "session");

            AgentExecutionResult failureResult = AgentExecutionResult.failure(
                    "실행 실패", java.util.List.of("에러 메시지"));
            when(agent.execute(anyString(), anyString())).thenReturn(failureResult);

            // When
            AgentExecutionResult result = facade.runAgent(userId, request);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.errors()).contains("에러 메시지");
        }
    }
}
