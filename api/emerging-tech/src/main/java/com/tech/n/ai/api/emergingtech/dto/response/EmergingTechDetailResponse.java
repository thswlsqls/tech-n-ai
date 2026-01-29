package com.tech.n.ai.api.emergingtech.dto.response;

import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Emerging Tech 상세 응답 DTO
 */
@Builder
public record EmergingTechDetailResponse(
    String id,
    String provider,
    String updateType,
    String title,
    String summary,
    String url,
    LocalDateTime publishedAt,
    String sourceType,
    String status,
    String externalId,
    EmergingTechMetadataResponse metadata,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    @Builder
    public record EmergingTechMetadataResponse(
        String version,
        List<String> tags,
        String author,
        String githubRepo,
        Map<String, Object> additionalInfo
    ) {}

    /**
     * Document → Response 변환
     */
    public static EmergingTechDetailResponse from(EmergingTechDocument document) {
        if (document == null) {
            return null;
        }

        EmergingTechMetadataResponse metadataResponse = null;
        if (document.getMetadata() != null) {
            metadataResponse = EmergingTechMetadataResponse.builder()
                .version(document.getMetadata().getVersion())
                .tags(document.getMetadata().getTags())
                .author(document.getMetadata().getAuthor())
                .githubRepo(document.getMetadata().getGithubRepo())
                .additionalInfo(document.getMetadata().getAdditionalInfo())
                .build();
        }

        return EmergingTechDetailResponse.builder()
            .id(document.getId() != null ? document.getId().toHexString() : null)
            .provider(document.getProvider())
            .updateType(document.getUpdateType())
            .title(document.getTitle())
            .summary(document.getSummary())
            .url(document.getUrl())
            .publishedAt(document.getPublishedAt())
            .sourceType(document.getSourceType())
            .status(document.getStatus())
            .externalId(document.getExternalId())
            .metadata(metadataResponse)
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }
}
