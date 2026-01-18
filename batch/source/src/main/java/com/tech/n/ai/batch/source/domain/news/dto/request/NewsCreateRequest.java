package com.tech.n.ai.batch.source.domain.news.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * News 생성 요청 DTO (내부 API용)
 * api-news 모듈의 DTO와 필드가 같아도 별도 정의
 */
@Builder
public record NewsCreateRequest(
    @NotBlank(message = "sourceId는 필수입니다.")
    String sourceId,
    
    @NotBlank(message = "제목은 필수입니다.")
    String title,
    
    String content,
    String summary,
    
    @NotNull(message = "발행일은 필수입니다.")
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
    @Builder
    public record NewsMetadataRequest(
        String sourceName,
        java.util.List<String> tags,
        Integer viewCount,
        Integer likeCount
    ) {
    }
}
