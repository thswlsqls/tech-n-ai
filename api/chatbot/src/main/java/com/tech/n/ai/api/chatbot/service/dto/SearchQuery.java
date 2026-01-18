package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * 검색 쿼리 DTO
 */
@Builder
public record SearchQuery(
    String query,
    SearchContext context
) {}
