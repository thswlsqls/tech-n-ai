package com.tech.n.ai.client.feign.domain.agent.contract;

/**
 * Agent API DTO
 */
public final class AgentDto {

    private AgentDto() {}

    public record AgentRunRequest(
        String goal,
        String sessionId
    ) {}
}
