package com.tech.n.ai.api.agent.controller;

import com.tech.n.ai.api.agent.agent.AgentExecutionResult;
import com.tech.n.ai.api.agent.dto.request.AgentRunRequest;
import com.tech.n.ai.api.agent.facade.AgentFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Emerging Tech Agent REST API 컨트롤러
 * Gateway에서 JWT 역할 기반(ADMIN) 인증을 수행합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentFacade agentFacade;

    /**
     * Agent 수동 실행
     *
     * POST /api/v1/agent/run
     * 인증: Gateway에서 JWT ADMIN 역할 검증
     *
     * @param request goal (필수), sessionId (선택)
     * @param userId  Gateway가 주입한 사용자 ID 헤더
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<AgentExecutionResult>> runAgent(
            @Valid @RequestBody AgentRunRequest request,
            @RequestHeader("x-user-id") String userId) {

        AgentExecutionResult result = agentFacade.runAgent(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
