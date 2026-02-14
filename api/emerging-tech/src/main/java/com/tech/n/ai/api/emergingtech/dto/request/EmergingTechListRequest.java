package com.tech.n.ai.api.emergingtech.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Emerging Tech 목록 조회 요청 DTO
 */
public record EmergingTechListRequest(
    @Min(value = 1, message = "page는 1 이상이어야 합니다.") Integer page,
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = 100, message = "size는 100 이하여야 합니다.") Integer size,
    String provider,     // 필터: TechProvider enum value
    String updateType,   // 필터: EmergingTechType enum value
    String status,       // 필터: PostStatus enum value
    String sort,         // 정렬: "publishedAt,desc"
    String startDate,    // 조회 시작일 (YYYY-MM-DD)
    String endDate,      // 조회 종료일 (YYYY-MM-DD)
    String sourceType    // 필터: SourceType enum value
) {
    // 파라미터 미제공 시 기본값 적용
    public EmergingTechListRequest {
        if (page == null) page = 1;
        if (size == null) size = 20;
    }
}
