package com.tech.n.ai.api.agent.tool.handler;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * LangChain4j Tool Error Handler 구현
 * AiServices.builder()에서 사용되는 3종 에러 핸들러를 제공
 *
 * <p>핸들러 목록:
 * <ul>
 *   <li>toolExecutionExceptionHandler: Tool 실행 중 예외 처리</li>
 *   <li>toolArgumentsErrorHandler: Tool 인자 오류 처리</li>
 *   <li>hallucinatedToolNameStrategy: 존재하지 않는 Tool 호출 처리</li>
 * </ul>
 */
@Slf4j
public final class ToolErrorHandlers {

    private ToolErrorHandlers() {
        // 유틸리티 클래스
    }

    /**
     * Tool 실행 중 예외 발생 시 핸들러
     * LLM이 에러를 인지하고 재시도하거나 다른 접근법을 선택할 수 있도록 명확한 에러 메시지 반환
     *
     * @param request Tool 실행 요청 정보
     * @param throwable 발생한 예외
     * @return LLM에게 전달할 에러 메시지
     */
    public static String handleToolExecutionError(ToolExecutionRequest request, Throwable throwable) {
        log.error("Tool 실행 중 예외 발생: tool={}, arguments={}, error={}",
                request.name(),
                request.arguments(),
                throwable.getMessage(),
                throwable);

        return String.format("Tool '%s' 실행 실패: %s. 다른 방법을 시도해주세요.",
                request.name(),
                throwable.getMessage());
    }

    /**
     * Tool 인자 오류 발생 시 핸들러
     * JSON 파싱 실패, 타입 불일치 등의 인자 관련 오류 처리
     *
     * @param request Tool 실행 요청 정보
     * @param error 발생한 오류
     * @return LLM에게 전달할 에러 메시지
     */
    public static String handleToolArgumentsError(ToolExecutionRequest request, Throwable error) {
        log.warn("Tool 인자 오류: tool={}, arguments={}, error={}",
                request.name(),
                request.arguments(),
                error.getMessage());

        return String.format("Tool '%s' 인자 오류: %s. 올바른 형식으로 다시 시도해주세요.",
                request.name(),
                error.getMessage());
    }

    /**
     * 존재하지 않는 Tool 호출 시 핸들러 (Hallucination 처리)
     * hallucinatedToolNameStrategy에서 요구하는 시그니처: Function&lt;ToolExecutionRequest, ToolExecutionResultMessage&gt;
     *
     * @param request Tool 실행 요청 정보
     * @return ToolExecutionResultMessage 에러 메시지
     */
    public static ToolExecutionResultMessage handleHallucinatedToolName(ToolExecutionRequest request) {
        String toolName = request.name();
        log.warn("존재하지 않는 Tool 호출 시도: {}", toolName);

        String errorMessage = String.format("Error: Tool '%s'은(는) 존재하지 않습니다. " +
                        "사용 가능한 Tool: fetch_github_releases, scrape_web_page, search_ai_updates, " +
                        "create_draft_post, publish_post, send_slack_notification",
                toolName);

        return ToolExecutionResultMessage.from(request, errorMessage);
    }
}
