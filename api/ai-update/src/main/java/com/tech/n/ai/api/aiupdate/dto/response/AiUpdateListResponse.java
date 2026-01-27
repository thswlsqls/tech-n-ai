package com.tech.n.ai.api.aiupdate.dto.response;

import com.tech.n.ai.common.core.dto.PageData;
import lombok.Builder;

import java.util.List;

/**
 * AI Update 목록 응답 DTO
 */
@Builder
public record AiUpdateListResponse(
    int pageSize,
    int pageNumber,
    int totalCount,
    List<AiUpdateDetailResponse> items
) {

    /**
     * PageData → ListResponse 변환
     */
    public static AiUpdateListResponse from(PageData<AiUpdateDetailResponse> pageData) {
        return AiUpdateListResponse.builder()
            .pageSize(pageData.pageSize())
            .pageNumber(pageData.pageNumber())
            .totalCount(pageData.totalSize())
            .items(pageData.list())
            .build();
    }
}
