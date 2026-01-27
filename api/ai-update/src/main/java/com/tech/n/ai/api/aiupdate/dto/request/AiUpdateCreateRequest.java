package com.tech.n.ai.api.aiupdate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI Update 생성 요청 DTO
 */
@Builder
public record AiUpdateCreateRequest(
    @NotNull String provider,       // AiProvider enum value
    @NotNull String updateType,     // AiUpdateType enum value
    @NotBlank String title,
    String summary,
    @NotBlank String url,
    LocalDateTime publishedAt,
    @NotNull String sourceType,     // SourceType enum value
    @NotNull String status,         // PostStatus enum value
    String externalId,              // 중복 체크용
    AiUpdateMetadataRequest metadata
) {

    @Builder
    public record AiUpdateMetadataRequest(
        String version,
        List<String> tags,
        String author,
        String githubRepo,
        Map<String, Object> additionalInfo
    ) {}
}
