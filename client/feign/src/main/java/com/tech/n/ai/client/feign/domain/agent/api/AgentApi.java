package com.tech.n.ai.client.feign.domain.agent.api;

import com.tech.n.ai.client.feign.domain.agent.client.AgentFeignClient;
import com.tech.n.ai.client.feign.domain.agent.contract.AgentContract;
import com.tech.n.ai.client.feign.domain.agent.contract.AgentDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * Agent API 구현체
 */
@RequiredArgsConstructor
public class AgentApi implements AgentContract {

    private final AgentFeignClient feignClient;

    @Override
    public ApiResponse<Object> runAgent(String userId, String userRole, AgentDto.AgentRunRequest request) {
        return feignClient.runAgent(userId, userRole, request);
    }
}
