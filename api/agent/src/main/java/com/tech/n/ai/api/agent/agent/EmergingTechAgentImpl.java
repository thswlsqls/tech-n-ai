package com.tech.n.ai.api.agent.agent;


import com.tech.n.ai.api.agent.config.AgentPromptConfig;
import com.tech.n.ai.api.agent.metrics.ToolExecutionMetrics;
import com.tech.n.ai.api.agent.tool.EmergingTechAgentTools;
import com.tech.n.ai.api.agent.tool.handler.ToolErrorHandlers;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


/**
 * Emerging Tech 추적 Agent 구현체
 * LangChain4j AiServices를 사용하여 Tool 기반 자율 실행
 *
 * <p>주요 특징:
 * <ul>
 *   <li>3종 Error Handler로 안전한 Tool 실행 보장</li>
 *   <li>@MemoryId 기반 세션 분리로 멀티 유저 지원</li>
 *   <li>입력값 검증으로 LLM hallucination 방어</li>
 *   <li>ThreadLocal 기반 메트릭으로 동시 실행 격리</li>
 * </ul>
 */
@Slf4j
@Service
public class EmergingTechAgentImpl implements EmergingTechAgent {

    private final ChatModel chatModel;
    private final EmergingTechAgentTools tools;
    private final AgentPromptConfig promptConfig;
    private final SlackContract slackContract;

    private static final int MAX_MESSAGES = 30;

    public EmergingTechAgentImpl(
            @Qualifier("agentChatModel") ChatModel chatModel,
            EmergingTechAgentTools tools,
            AgentPromptConfig promptConfig,
            SlackContract slackContract) {
        this.chatModel = chatModel;
        this.tools = tools;
        this.promptConfig = promptConfig;
        this.slackContract = slackContract;
    }

    @Override
    public AgentExecutionResult execute(String goal) {
        return execute(goal, generateSessionId());
    }

    @Override
    public AgentExecutionResult execute(String goal, String sessionId) {
        long startTime = System.currentTimeMillis();

        // 실행별 메트릭 생성 및 바인딩 (동시 실행 격리)
        ToolExecutionMetrics metrics = new ToolExecutionMetrics();
        tools.bindMetrics(metrics);

        try {
            // 세션별 ChatMemory 제공자 (멀티 유저 지원)
            ChatMemoryProvider memoryProvider = memoryId -> MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(MAX_MESSAGES)
                    .build();

            AgentAssistant assistant = AiServices.builder(AgentAssistant.class)
                    .chatModel(chatModel)
                    .tools(tools)
                    .chatMemoryProvider(memoryProvider)
                    // Error Handler 1: Tool 실행 중 예외 처리
                    .toolExecutionErrorHandler(ToolErrorHandlers::handleToolExecutionError)
                    // Error Handler 2: Tool 인자 오류 처리
                    .toolArgumentsErrorHandler(ToolErrorHandlers::handleToolArgumentsError)
                    // Error Handler 3: 존재하지 않는 Tool 호출 처리 (Hallucination)
                    .hallucinatedToolNameStrategy(ToolErrorHandlers::handleHallucinatedToolName)
                    .build();

            String response = assistant.chat(sessionId, promptConfig.buildPrompt(goal));

            long elapsed = System.currentTimeMillis() - startTime;
            int toolCallCount = metrics.getToolCallCount();
            int analyticsCallCount = metrics.getAnalyticsCallCount();
            int validationErrors = metrics.getValidationErrorCount();

            log.info("Agent 실행 완료: goal={}, sessionId={}, toolCalls={}, analyticsCalls={}, validationErrors={}, elapsed={}ms",
                    goal, sessionId, toolCallCount, analyticsCallCount, validationErrors, elapsed);

            return AgentExecutionResult.success(response, toolCallCount, analyticsCallCount, elapsed);

        } catch (Exception e) {
            log.error("Agent 실행 실패: goal={}, sessionId={}", goal, sessionId, e);
            notifyError(goal, e);
            return AgentExecutionResult.failure(
                    "Agent 실행 중 오류 발생: " + e.getMessage(),
                    List.of(e.getMessage())
            );
        } finally {
            // ThreadLocal 메트릭 해제 (메모리 누수 방지)
            tools.unbindMetrics();
        }
    }

    private String generateSessionId() {
        return "agent-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void notifyError(String goal, Exception e) {
        try {
            slackContract.sendErrorNotification("Agent 실행 실패\nGoal: " + goal, e);
        } catch (Exception slackError) {
            log.error("Slack 에러 알림 전송 실패", slackError);
        }
    }
}
