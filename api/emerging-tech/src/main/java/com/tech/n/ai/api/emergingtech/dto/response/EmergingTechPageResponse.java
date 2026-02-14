package com.tech.n.ai.api.emergingtech.dto.response;

import com.tech.n.ai.common.core.dto.PageData;
import lombok.Builder;

import java.util.List;

/**
 * Emerging Tech 페이지네이션 응답 DTO (목록/검색 공통)
 */
@Builder
public record EmergingTechPageResponse(
    int pageSize,
    int pageNumber,
    int totalCount,
    List<EmergingTechDetailResponse> items
) {

    /**
     * PageData → PageResponse 변환
     */
    public static EmergingTechPageResponse from(PageData<EmergingTechDetailResponse> pageData) {
        return EmergingTechPageResponse.builder()
            .pageSize(pageData.pageSize())
            .pageNumber(pageData.pageNumber())
            .totalCount(pageData.totalSize())
            .items(pageData.list())
            .build();
    }
}
