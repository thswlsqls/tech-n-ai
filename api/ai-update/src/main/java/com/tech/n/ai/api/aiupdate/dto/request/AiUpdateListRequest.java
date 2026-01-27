package com.tech.n.ai.api.aiupdate.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * AI Update 목록 조회 요청 DTO
 */
public record AiUpdateListRequest(
    @Min(1) int page,
    @Min(1) @Max(100) int size,
    String provider,     // 필터: AiProvider enum value
    String updateType,   // 필터: AiUpdateType enum value
    String status,       // 필터: PostStatus enum value
    String sort          // 정렬: "publishedAt,desc"
) {
    public AiUpdateListRequest {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
