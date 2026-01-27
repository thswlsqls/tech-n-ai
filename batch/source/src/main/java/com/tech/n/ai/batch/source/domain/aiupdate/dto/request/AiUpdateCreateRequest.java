package com.tech.n.ai.batch.source.domain.aiupdate.dto.request;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Update 생성 요청 DTO (Batch용)
 */
@Builder
public record AiUpdateCreateRequest(
    String provider,       // AiProvider enum value
    String updateType,     // AiUpdateType enum value
    String title,
    String summary,
    String url,
    LocalDateTime publishedAt,
    String sourceType,     // SourceType enum value
    String status,         // PostStatus enum value
    String externalId,     // 중복 체크용
    AiUpdateMetadataRequest metadata
) {

    @Builder
    public record AiUpdateMetadataRequest(
        String version,
        List<String> tags,
        String author,
        String githubRepo
    ) {}
}
