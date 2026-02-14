package com.tech.n.ai.api.emergingtech.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Emerging Tech 검색 요청 DTO
 */
public record EmergingTechSearchRequest(
    @NotBlank(message = "검색어는 필수입니다.") String q,
    @Min(value = 1, message = "page는 1 이상이어야 합니다.") Integer page,
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = 100, message = "size는 100 이하여야 합니다.") Integer size
) {
    // 파라미터 미제공 시 기본값 적용
    public EmergingTechSearchRequest {
        if (page == null) page = 1;
        if (size == null) size = 20;
    }
}
