package com.tech.n.ai.api.archive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 아카이브 히스토리 목록 조회 요청 DTO
 */
public record ArchiveHistoryListRequest(
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    Integer page,
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    Integer size,
    
    String operationType,
    String startDate,
    String endDate
) {
    public ArchiveHistoryListRequest {
        if (page == null) {
            page = 1;
        }
        if (size == null) {
            size = 10;
        }
    }
}
