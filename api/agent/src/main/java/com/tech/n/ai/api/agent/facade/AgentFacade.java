package com.tech.n.ai.api.agent.facade;

import com.tech.n.ai.api.agent.agent.AgentExecutionResult;
import com.tech.n.ai.api.agent.agent.EmergingTechAgent;
import com.tech.n.ai.api.agent.dto.request.AgentRunRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Agent Facade
 * Controller와 Agent 사이의 오케스트레이션 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentFacade {

    private final EmergingTechAgent agent;

    /**
     * Agent 실행
     *
     * @param userId 사용자 ID (로깅 및 세션 ID 생성용)
     * @param request 실행 요청
     * @return 실행 결과
     */
    public AgentExecutionResult runAgent(String userId, AgentRunRequest request) {
        String sessionId = resolveSessionId(userId, request.sessionId());

        log.info("Agent 실행 요청: userId={}, goal={}, sessionId={}",
                userId, request.goal(), sessionId);

        return agent.execute(request.goal(), sessionId);
    }

    /**
     * 세션 ID 결정: 요청에 포함되어 있으면 사용, 없으면 자동 생성
     */
    private String resolveSessionId(String userId, String requestSessionId) {
        if (requestSessionId != null && !requestSessionId.isBlank()) {
            return requestSessionId;
        }
        return "admin-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
