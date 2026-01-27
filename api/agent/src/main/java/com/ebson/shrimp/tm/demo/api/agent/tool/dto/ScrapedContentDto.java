package com.tech.n.ai.api.agent.tool.dto;

/**
 * Scraped Content Tool 응답 DTO
 */
public record ScrapedContentDto(
    String title,
    String content,
    String url
) {}
