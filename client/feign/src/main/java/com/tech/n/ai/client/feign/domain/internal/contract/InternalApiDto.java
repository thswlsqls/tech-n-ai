package com.tech.n.ai.client.feign.domain.internal.contract;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내부 API 호출용 DTO (client-feign 모듈에서 독립적으로 정의)
 * api-contest, api-news 모듈의 DTO와 필드가 같아도 별도 정의
 * 
 * DTO 독립성 원칙: 각 모듈에서 독립적으로 DTO 정의 (모듈 간 DTO 공유 금지)
 */
public class InternalApiDto {
    
    // ========== Contest 관련 DTO ==========
    
    /**
     * Contest 생성 요청 DTO
     * api-contest 모듈의 ContestCreateRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class ContestCreateRequest {
        private String sourceId;
        private String title;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String description;
        private String url;
        private ContestMetadataRequest metadata;
    }
    
    /**
     * Contest 다건 생성 요청 DTO
     * api-contest 모듈의 ContestBatchRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class ContestBatchRequest {
        private List<ContestCreateRequest> contests;
    }
    
    /**
     * Contest Metadata 요청 DTO
     * api-contest 모듈의 ContestMetadataRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class ContestMetadataRequest {
        private String sourceName;
        private String prize;
        private Integer participants;
        private List<String> tags;
    }
    
    // ========== News 관련 DTO ==========
    
    /**
     * News 생성 요청 DTO
     * api-news 모듈의 NewsCreateRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class NewsCreateRequest {
        private String sourceId;
        private String title;
        private String content;
        private String summary;
        private LocalDateTime publishedAt;
        private String url;
        private String author;
        private NewsMetadataRequest metadata;
    }
    
    /**
     * News 다건 생성 요청 DTO
     * api-news 모듈의 NewsBatchRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class NewsBatchRequest {
        private List<NewsCreateRequest> newsArticles;
    }
    
    /**
     * News Metadata 요청 DTO
     * api-news 모듈의 NewsMetadataRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class NewsMetadataRequest {
        private String sourceName;
        private List<String> tags;
        private Integer viewCount;
        private Integer likeCount;
    }

    // ========== AI Update 관련 DTO ==========

    /**
     * AI Update 생성 요청 DTO
     * api-ai-update 모듈의 AiUpdateCreateRequest와 필드가 같아도 별도 정의
     */
    @Data
    @Builder
    public static class AiUpdateCreateRequest {
        private String provider;      // AiProvider enum value
        private String updateType;    // AiUpdateType enum value
        private String title;
        private String summary;
        private String url;
        private LocalDateTime publishedAt;
        private String sourceType;    // SourceType enum value
        private String status;        // PostStatus enum value
        private String externalId;    // 중복 체크용
        private AiUpdateMetadataRequest metadata;
    }

    /**
     * AI Update 다건 생성 요청 DTO
     */
    @Data
    @Builder
    public static class AiUpdateBatchRequest {
        private List<AiUpdateCreateRequest> items;
    }

    /**
     * AI Update Metadata 요청 DTO
     */
    @Data
    @Builder
    public static class AiUpdateMetadataRequest {
        private String version;
        private List<String> tags;
        private String author;
        private String githubRepo;
    }
}
