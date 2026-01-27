package com.tech.n.ai.api.agent.agent;

import java.util.List;

/**
 * Agent 실행 결과 DTO
 */
public record AgentExecutionResult(
    boolean success,
    String summary,
    int toolCallCount,
    int postsCreated,
    long executionTimeMs,
    List<String> errors
) {
    public static AgentExecutionResult success(String summary, int toolCallCount, int postsCreated, long executionTimeMs) {
        return new AgentExecutionResult(true, summary, toolCallCount, postsCreated, executionTimeMs, List.of());
    }

    public static AgentExecutionResult failure(String summary, List<String> errors) {
        return new AgentExecutionResult(false, summary, 0, 0, 0, errors);
    }
}
