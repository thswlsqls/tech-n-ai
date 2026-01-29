package com.tech.n.ai.api.emergingtech.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Emerging Tech 목록 조회 요청 DTO
 */
public record EmergingTechListRequest(
    @Min(1) int page,
    @Min(1) @Max(100) int size,
    String provider,     // 필터: TechProvider enum value
    String updateType,   // 필터: EmergingTechType enum value
    String status,       // 필터: PostStatus enum value
    String sort          // 정렬: "publishedAt,desc"
) {
    public EmergingTechListRequest {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
