package com.tech.n.ai.api.agent.agent;

import com.tech.n.ai.api.agent.tool.AiUpdateAgentTools;
import com.tech.n.ai.api.agent.tool.handler.ToolErrorHandlers;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * AI 업데이트 추적 Agent 구현체
 * LangChain4j AiServices를 사용하여 Tool 기반 자율 실행
 *
 * <p>주요 특징:
 * <ul>
 *   <li>3종 Error Handler로 안전한 Tool 실행 보장</li>
 *   <li>@MemoryId 기반 세션 분리로 멀티 유저 지원</li>
 *   <li>입력값 검증으로 LLM hallucination 방어</li>
 * </ul>
 */
@Slf4j
@Service
public class AiUpdateAgentImpl implements AiUpdateAgent {

    private final ChatLanguageModel chatModel;
    private final AiUpdateAgentTools tools;
    private final SlackContract slackContract;

    private static final int MAX_MESSAGES = 30;

    public AiUpdateAgentImpl(
            @Qualifier("agentChatModel") ChatLanguageModel chatModel,
            AiUpdateAgentTools tools,
            SlackContract slackContract) {
        this.chatModel = chatModel;
        this.tools = tools;
        this.slackContract = slackContract;
    }

    /**
     * Agent 실행 (세션 ID 자동 생성)
     *
     * @param goal 실행 목표
     * @return 실행 결과
     */
    @Override
    public AgentExecutionResult execute(String goal) {
        String sessionId = generateSessionId();
        return execute(goal, sessionId);
    }

    /**
     * Agent 실행 (세션 ID 지정)
     *
     * @param goal 실행 목표
     * @param sessionId 세션 식별자 (멀티 유저 지원)
     * @return 실행 결과
     */
    public AgentExecutionResult execute(String goal, String sessionId) {
        long startTime = System.currentTimeMillis();

        tools.resetCounters();

        try {
            // 세션별 ChatMemory 제공자 (멀티 유저 지원)
            ChatMemoryProvider memoryProvider = memoryId -> MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(MAX_MESSAGES)
                    .build();

            AgentAssistant assistant = AiServices.builder(AgentAssistant.class)
                    .chatLanguageModel(chatModel)
                    .tools(tools)
                    .chatMemoryProvider(memoryProvider)
                    // Error Handler 1: Tool 실행 중 예외 처리
                    .toolExecutionExceptionHandler(ToolErrorHandlers::handleToolExecutionError)
                    // Error Handler 2: Tool 인자 오류 처리
                    .toolArgumentsErrorHandler(ToolErrorHandlers::handleToolArgumentsError)
                    // Error Handler 3: 존재하지 않는 Tool 호출 처리 (Hallucination)
                    .hallucinatedToolNameStrategy(ToolErrorHandlers::handleHallucinatedToolName)
                    .build();

            String response = assistant.chat(sessionId, buildPrompt(goal));

            long elapsed = System.currentTimeMillis() - startTime;
            int toolCallCount = tools.getToolCallCount();
            int postsCreated = tools.getPostsCreatedCount();
            int validationErrors = tools.getValidationErrorCount();

            log.info("Agent 실행 완료: goal={}, sessionId={}, toolCalls={}, postsCreated={}, validationErrors={}, elapsed={}ms",
                    goal, sessionId, toolCallCount, postsCreated, validationErrors, elapsed);

            return AgentExecutionResult.success(response, toolCallCount, postsCreated, elapsed);

        } catch (Exception e) {
            log.error("Agent 실행 실패: goal={}, sessionId={}", goal, sessionId, e);
            notifyError(goal, e);
            return AgentExecutionResult.failure(
                    "Agent 실행 중 오류 발생: " + e.getMessage(),
                    List.of(e.getMessage())
            );
        }
    }

    /**
     * 세션 ID 생성
     */
    private String generateSessionId() {
        return "agent-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String buildPrompt(String goal) {
        return """
            당신은 AI 업데이트 추적 전문가입니다.

            ## 역할
            - 빅테크 AI 서비스(OpenAI, Anthropic, Google, Meta)의 최신 업데이트 추적
            - 중요한 업데이트를 식별하고 초안 포스팅

            ## 사용 가능한 도구
            - fetch_github_releases: GitHub 저장소 릴리스 조회
            - scrape_web_page: 웹 페이지 크롤링
            - search_ai_updates: 기존 업데이트 검색 (중복 방지)
            - create_draft_post: 초안 포스트 생성 (DRAFT 상태)
            - publish_post: 포스트 게시 (승인)
            - send_slack_notification: Slack 알림 전송

            ## 주요 저장소 정보
            - OpenAI: openai/openai-python
            - Anthropic: anthropics/anthropic-sdk-python
            - Google: google/generative-ai-python
            - Meta: facebookresearch/llama

            ## 규칙
            1. 작업 전 항상 search_ai_updates로 중복 확인
            2. 중요도가 높은 업데이트만 포스팅 (모델 출시, 주요 API 변경, 메이저 버전)
            3. 마이너 버그 수정이나 문서 업데이트는 건너뛰기
            4. 초안 생성 후 Slack으로 관리자에게 알림
            5. 작업 완료 후 결과 요약 제공

            ## 사용자 요청
            %s
            """.formatted(goal);
    }

    private void notifyError(String goal, Exception e) {
        try {
            slackContract.sendErrorNotification("Agent 실행 실패\nGoal: " + goal, e);
        } catch (Exception slackError) {
            log.error("Slack 에러 알림 전송 실패", slackError);
        }
    }
}
