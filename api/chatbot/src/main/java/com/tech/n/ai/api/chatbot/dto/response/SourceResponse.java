package com.tech.n.ai.api.chatbot.dto.response;

import lombok.Builder;

/**
 * 소스 응답 DTO
 */
@Builder
public record SourceResponse(
    String documentId,
    String collectionType,
    Double score
) {}
