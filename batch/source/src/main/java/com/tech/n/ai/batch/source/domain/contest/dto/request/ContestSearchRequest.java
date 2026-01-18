package com.tech.n.ai.batch.source.domain.contest.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Contest 검색 요청 DTO
 */
public record ContestSearchRequest(
    @NotBlank(message = "검색어는 필수입니다.")
    String q,
    
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    Integer page,
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    Integer size
) {
    public ContestSearchRequest {
        if (page == null) {
            page = 1;
        }
        if (size == null) {
            size = 10;
        }
    }
}
