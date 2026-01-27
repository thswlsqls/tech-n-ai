package com.tech.n.ai.api.agent.agent;

/**
 * AI 업데이트 추적 Agent 인터페이스
 * 자연어 목표를 받아 자율적으로 데이터 수집 및 포스팅 수행
 */
public interface AiUpdateAgent {

    /**
     * 자연어 목표 실행
     *
     * @param goal 실행 목표 (예: "OpenAI와 Anthropic 최신 업데이트 확인하고 중요한 것만 포스팅해줘")
     * @return 실행 결과
     */
    AgentExecutionResult execute(String goal);
}
