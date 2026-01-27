package com.tech.n.ai.api.news.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * News 생성 요청 DTO (내부 API용)
 */
public record NewsCreateRequest(
    @NotBlank(message = "sourceId는 필수입니다.")
    String sourceId,
    
    @NotBlank(message = "제목은 필수입니다.")
    String title,
    
    String content,
    String summary,
    
//    @NotNull(message = "발행일은 필수입니다.")
    LocalDateTime publishedAt,
    
    @NotBlank(message = "URL은 필수입니다.")
    String url,
    
    String author,
    
    @Valid
    NewsMetadataRequest metadata
) {
    /**
     * NewsMetadata 요청 DTO
     */
    public record NewsMetadataRequest(
        String sourceName,
        java.util.List<String> tags,
        Integer viewCount,
        Integer likeCount
    ) {
    }
}
