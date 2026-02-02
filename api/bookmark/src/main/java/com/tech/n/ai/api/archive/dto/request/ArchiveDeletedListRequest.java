package com.tech.n.ai.api.archive.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 삭제된 아카이브 목록 조회 요청 DTO
 */
public record ArchiveDeletedListRequest(
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    Integer page,
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    Integer size,
    
    Integer days
) {
    public ArchiveDeletedListRequest {
        if (page == null) {
            page = 1;
        }
        if (size == null) {
            size = 10;
        }
        if (days == null) {
            days = 30;
        }
    }
}
