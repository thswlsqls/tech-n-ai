package com.tech.n.ai.client.feign.domain.agent.contract;

import com.tech.n.ai.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Agent API Contract 인터페이스
 * Chatbot 모듈에서 Agent 모듈의 API 호출용
 */
public interface AgentContract {

    @PostMapping("/api/v1/agent/run")
    ApiResponse<Object> runAgent(
        @RequestHeader("x-user-id") String userId,
        @RequestHeader("x-user-role") String userRole,
        @RequestBody AgentDto.AgentRunRequest request);
}
