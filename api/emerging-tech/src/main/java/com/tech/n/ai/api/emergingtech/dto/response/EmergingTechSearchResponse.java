package com.tech.n.ai.api.emergingtech.dto.response;

import com.tech.n.ai.common.core.dto.PageData;
import lombok.Builder;

import java.util.List;

/**
 * Emerging Tech 검색 응답 DTO
 */
@Builder
public record EmergingTechSearchResponse(
    int pageSize,
    int pageNumber,
    int totalCount,
    List<EmergingTechDetailResponse> items
) {

    /**
     * PageData → SearchResponse 변환
     */
    public static EmergingTechSearchResponse from(PageData<EmergingTechDetailResponse> pageData) {
        return EmergingTechSearchResponse.builder()
            .pageSize(pageData.pageSize())
            .pageNumber(pageData.pageNumber())
            .totalCount(pageData.totalSize())
            .items(pageData.list())
            .build();
    }
}
