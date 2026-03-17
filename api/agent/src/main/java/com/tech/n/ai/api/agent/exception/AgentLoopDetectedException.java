package com.tech.n.ai.api.agent.exception;

/**
 * Agent Tool 호출 루프 감지 시 발생하는 예외
 * LLM이 동일한 차단된 Tool을 반복 호출할 때 langchain4j 루프를 강제 탈출
 */
public class AgentLoopDetectedException extends RuntimeException {

    public AgentLoopDetectedException(String message) {
        super(message);
    }
}
