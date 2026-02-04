package com.tech.n.ai.api.agent.dto.request;

import jakarta.validation.constraints.NotBlank;

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
) {
}
