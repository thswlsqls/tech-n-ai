package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * 정제된 검색 결과 DTO
 */
@Builder
public record RefinedResult(
    String documentId,
    String text,
    Double score,
    String collectionType,
    Object metadata
) {}
