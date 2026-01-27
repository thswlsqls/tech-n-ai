package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * Web 검색 결과 문서
 */
@Builder
public record WebSearchDocument(
    String title,
    String url,
    String snippet,
    String source
) {}
