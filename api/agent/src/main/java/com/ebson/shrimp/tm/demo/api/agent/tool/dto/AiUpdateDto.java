package com.tech.n.ai.api.agent.tool.dto;

/**
 * AI Update Tool 응답 DTO
 */
public record AiUpdateDto(
    String id,
    String provider,
    String updateType,
    String title,
    String url,
    String status
) {}
