package com.tech.n.ai.api.chatbot.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 챗봇 응답 DTO
 */
@Builder
public record ChatResponse(
    String response,
    String conversationId,
    List<SourceResponse> sources
) {}
