package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * 토큰 사용량 정보
 */
@Builder
public record TokenUsage(
    String requestId,
    String userId,
    Integer inputTokens,
    Integer outputTokens,
    Integer totalTokens,
    Instant timestamp
) {}
