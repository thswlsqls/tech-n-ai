package com.tech.n.ai.api.aiupdate.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * AI Update 검색 요청 DTO
 */
public record AiUpdateSearchRequest(
    @NotBlank String q,  // 검색어
    @Min(1) int page,
    @Min(1) @Max(100) int size
) {
    public AiUpdateSearchRequest {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
