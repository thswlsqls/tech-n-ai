package com.tech.n.ai.client.feign.domain.internal.contract;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내부 API 호출용 DTO (client-feign 모듈에서 독립적으로 정의)
 *
 * DTO 독립성 원칙: 각 모듈에서 독립적으로 DTO 정의 (모듈 간 DTO 공유 금지)
 */
public class InternalApiDto {

    // ========== Emerging Tech 관련 DTO ==========

    /**
     * Emerging Tech 생성 요청 DTO
     * api-emerging-tech 모듈의 EmergingTechCreateRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class EmergingTechCreateRequest {
        private String provider;      // TechProvider enum value
        private String updateType;    // EmergingTechType enum value
        private String title;
        private String summary;
        private String url;
        private LocalDateTime publishedAt;
        private String sourceType;    // SourceType enum value
        private String status;        // PostStatus enum value
        private String externalId;    // 중복 체크용
        private EmergingTechMetadataRequest metadata;
    }

    /**
     * Emerging Tech 다건 생성 요청 DTO
     */
    @Data
    @Builder
    public static class EmergingTechBatchRequest {
        private List<EmergingTechCreateRequest> items;
    }

    /**
     * Emerging Tech Metadata 요청 DTO
     */
    @Data
    @Builder
    public static class EmergingTechMetadataRequest {
        private String version;
        private List<String> tags;
        private String author;
        private String githubRepo;
    }

    // ========== Emerging Tech 응답 DTO ==========

    /**
     * Emerging Tech 단건 응답 DTO
     */
    @Data
    @Builder
    public static class EmergingTechDetailResponse {
        private String id;
        private String provider;
        private String updateType;
        private String title;
        private String summary;
        private String url;
        private LocalDateTime publishedAt;
        private String sourceType;
        private String status;
        private String externalId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Emerging Tech 다건 생성 응답 DTO
     */
    @Data
    @Builder
    public static class EmergingTechBatchResponse {
        private int totalCount;
        private int successCount;
        private int newCount;
        private int duplicateCount;
        private int failureCount;
        private List<String> failureMessages;
    }
}
