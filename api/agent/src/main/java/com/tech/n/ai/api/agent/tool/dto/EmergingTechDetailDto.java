package com.tech.n.ai.api.agent.tool.dto;

import java.util.List;

/**
 * 상세 조회 결과 DTO
 * EmergingTechDto보다 summary, publishedAt, sourceType, metadata 등 추가 정보 포함
 */
public record EmergingTechDetailDto(
    String id,
    String provider,
    String updateType,
    String title,
    String summary,
    String url,
    String publishedAt,
    String sourceType,
    String status,
    String externalId,
    String createdAt,
    String updatedAt,
    EmergingTechMetadataDto metadata
) {
    public record EmergingTechMetadataDto(
        String version,
        List<String> tags,
        String author,
        String githubRepo
    ) {}

    public static EmergingTechDetailDto notFound(String id) {
        return new EmergingTechDetailDto(
            id, null, null, null,
            "해당 ID의 도큐먼트를 찾을 수 없습니다: " + id,
            null, null, null, null, null, null, null, null
        );
    }
}
