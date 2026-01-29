package com.tech.n.ai.batch.source.domain.contest.dto.response;

import com.ebson.shrimp.tm.demo.common.core.dto.PageData;

/**
 * Contest 검색 응답 DTO
 */
public record ContestSearchResponse(
    PageData<ContestDetailResponse> data
) {
    /**
     * PageData로부터 ContestSearchResponse 생성
     * 
     * @param pageData PageData<ContestDetailResponse>
     * @return ContestSearchResponse
     */
    public static ContestSearchResponse from(PageData<ContestDetailResponse> pageData) {
        return new ContestSearchResponse(pageData);
    }
}
