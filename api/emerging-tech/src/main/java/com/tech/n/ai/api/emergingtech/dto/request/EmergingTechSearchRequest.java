package com.tech.n.ai.api.emergingtech.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Emerging Tech 검색 요청 DTO
 */
public record EmergingTechSearchRequest(
    @NotBlank String q,  // 검색어
    @Min(1) int page,
    @Min(1) @Max(100) int size
) {
    public EmergingTechSearchRequest {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
