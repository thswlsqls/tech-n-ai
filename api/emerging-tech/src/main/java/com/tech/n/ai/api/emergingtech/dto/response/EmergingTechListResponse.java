package com.tech.n.ai.api.emergingtech.dto.response;

import com.tech.n.ai.common.core.dto.PageData;
import lombok.Builder;

import java.util.List;

/**
 * Emerging Tech 목록 응답 DTO
 */
@Builder
public record EmergingTechListResponse(
    int pageSize,
    int pageNumber,
    int totalCount,
    List<EmergingTechDetailResponse> items
) {

    /**
     * PageData → ListResponse 변환
     */
    public static EmergingTechListResponse from(PageData<EmergingTechDetailResponse> pageData) {
        return EmergingTechListResponse.builder()
            .pageSize(pageData.pageSize())
            .pageNumber(pageData.pageNumber())
            .totalCount(pageData.totalSize())
            .items(pageData.list())
            .build();
    }
}
