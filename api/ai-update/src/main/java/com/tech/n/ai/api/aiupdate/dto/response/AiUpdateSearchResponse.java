package com.tech.n.ai.api.aiupdate.dto.response;

import com.tech.n.ai.common.core.dto.PageData;
import lombok.Builder;

import java.util.List;

/**
 * AI Update 검색 응답 DTO
 */
@Builder
public record AiUpdateSearchResponse(
    int pageSize,
    int pageNumber,
    int totalCount,
    List<AiUpdateDetailResponse> items
) {

    /**
     * PageData → SearchResponse 변환
     */
    public static AiUpdateSearchResponse from(PageData<AiUpdateDetailResponse> pageData) {
        return AiUpdateSearchResponse.builder()
            .pageSize(pageData.pageSize())
            .pageNumber(pageData.pageNumber())
            .totalCount(pageData.totalSize())
            .items(pageData.list())
            .build();
    }
}
