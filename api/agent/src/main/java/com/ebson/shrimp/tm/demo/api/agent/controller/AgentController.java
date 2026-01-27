package com.tech.n.ai.api.agent.controller;

import com.tech.n.ai.api.agent.agent.AgentExecutionResult;
import com.tech.n.ai.api.agent.agent.AiUpdateAgent;
import com.tech.n.ai.api.agent.agent.AiUpdateAgentImpl;
import com.tech.n.ai.api.agent.config.AgentConfig;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * AI Update Agent REST API 컨트롤러
 * 내부 API Key 인증을 통한 수동 실행 트리거
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AiUpdateAgent agent;
    private final AgentConfig agentConfig;

    /**
     * Agent 수동 실행
     *
     * POST /api/v1/agent/run
     * Header: X-Internal-Api-Key
     *
     * @param request goal (필수), sessionId (선택)
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<AgentExecutionResult>> runAgent(
            @Valid @RequestBody AgentRunRequest request,
            @RequestHeader("X-Internal-Api-Key") String requestApiKey
    ) {
        validateApiKey(requestApiKey);

        // sessionId 지정되지 않으면 자동 생성
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : "manual-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Agent 수동 실행 요청: goal={}, sessionId={}", request.goal(), sessionId);

        // sessionId가 지정된 경우 오버로드된 execute 메서드 호출
        AgentExecutionResult result;
        if (agent instanceof AiUpdateAgentImpl agentImpl) {
            result = agentImpl.execute(request.goal(), sessionId);
        } else {
            result = agent.execute(request.goal());
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void validateApiKey(String requestApiKey) {
        if (requestApiKey == null || requestApiKey.isBlank()) {
            throw new UnauthorizedException("내부 API 키가 제공되지 않았습니다.");
        }

        String configuredApiKey = agentConfig.getApiKey();
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            log.warn("내부 API 키가 설정되지 않았습니다. 설정 파일을 확인하세요.");
            throw new UnauthorizedException("내부 API 키가 설정되지 않았습니다.");
        }

        if (!MessageDigest.isEqual(configuredApiKey.getBytes(), requestApiKey.getBytes())) {
            throw new UnauthorizedException("유효하지 않은 내부 API 키입니다.");
        }
    }

    /**
     * Agent 실행 요청 DTO
     *
     * @param goal 실행 목표 (필수)
     * @param sessionId 세션 식별자 (선택, 미지정 시 자동 생성)
     */
    public record AgentRunRequest(
            @NotBlank(message = "goal은 필수입니다.")
            String goal,
            String sessionId
    ) {}
}
