package com.tech.n.ai.api.news.dto.response;

import com.tech.n.ai.datasource.mongodb.document.NewsArticleDocument;

import java.time.LocalDateTime;
import java.util.List;

/**
 * News 상세 조회 응답 DTO
 */
public record NewsDetailResponse(
    String id,
    String sourceId,
    String title,
    String content,
    String summary,
    LocalDateTime publishedAt,
    String url,
    String author,
    NewsMetadataResponse metadata,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    /**
     * NewsArticleDocument로부터 NewsDetailResponse 생성
     * 
     * @param document NewsArticleDocument
     * @return NewsDetailResponse
     */
    public static NewsDetailResponse from(NewsArticleDocument document) {
        if (document == null) {
            return null;
        }
        
        NewsMetadataResponse metadataResponse = null;
        if (document.getMetadata() != null) {
            metadataResponse = new NewsMetadataResponse(
                document.getMetadata().getSourceName(),
                document.getMetadata().getTags(),
                document.getMetadata().getViewCount(),
                document.getMetadata().getLikeCount()
            );
        }
        
        return new NewsDetailResponse(
            document.getId() != null ? document.getId().toHexString() : null,
            document.getSourceId() != null ? document.getSourceId().toHexString() : null,
            document.getTitle(),
            document.getContent(),
            document.getSummary(),
            document.getPublishedAt(),
            document.getUrl(),
            document.getAuthor(),
            metadataResponse,
            document.getCreatedAt(),
            document.getCreatedBy(),
            document.getUpdatedAt(),
            document.getUpdatedBy()
        );
    }
    
    /**
     * NewsMetadata 응답 DTO
     */
    public record NewsMetadataResponse(
        String sourceName,
        List<String> tags,
        Integer viewCount,
        Integer likeCount
    ) {
    }
}
