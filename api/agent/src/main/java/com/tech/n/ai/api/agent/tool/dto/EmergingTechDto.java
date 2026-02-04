package com.tech.n.ai.api.agent.tool.dto;

/**
 * Emerging Tech Tool 응답 DTO
 */
public record EmergingTechDto(
    String id,
    String provider,
    String updateType,
    String title,
    String url,
    String status
) {}
