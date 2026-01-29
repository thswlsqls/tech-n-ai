package com.tech.n.ai.batch.source.domain.emergingtech.dto.request;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Emerging Tech 생성 요청 DTO (Batch용)
 */
@Builder
public record EmergingTechCreateRequest(
    String provider,       // TechProvider enum value
    String updateType,     // EmergingTechType enum value
    String title,
    String summary,
    String url,
    LocalDateTime publishedAt,
    String sourceType,     // SourceType enum value
    String status,         // PostStatus enum value
    String externalId,     // 중복 체크용
    EmergingTechMetadataRequest metadata
) {

    @Builder
    public record EmergingTechMetadataRequest(
        String version,
        List<String> tags,
        String author,
        String githubRepo
    ) {}
}
