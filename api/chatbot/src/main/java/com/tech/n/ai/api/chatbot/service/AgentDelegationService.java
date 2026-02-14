package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.client.feign.domain.agent.contract.AgentContract;
import com.tech.n.ai.client.feign.domain.agent.contract.AgentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDelegationService {

    private final AgentContract agentApi;

    /**
     * Agent에게 작업 위임
     */
    public String delegateToAgent(String goal, Long userId, String userRole) {
        try {
            String sessionId = "chatbot-" + userId + "-" + System.currentTimeMillis();
            var request = new AgentDto.AgentRunRequest(goal, sessionId);
            var result = agentApi.runAgent(String.valueOf(userId), userRole, request);
            return formatResult(result.data());
        } catch (Exception e) {
            log.error("Agent delegation failed", e);
            return "Agent 작업 요청에 실패했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "Agent 작업이 완료되었습니다.";
        }
        return "Agent 작업이 완료되었습니다. 결과: " + result.toString();
    }
}
