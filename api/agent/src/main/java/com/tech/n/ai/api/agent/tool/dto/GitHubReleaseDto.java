package com.tech.n.ai.api.agent.tool.dto;

/**
 * GitHub Release Tool 응답 DTO
 */
public record GitHubReleaseDto(
    String tagName,
    String name,
    String body,
    String htmlUrl,
    String publishedAt
) {}
