package com.tech.n.ai.batch.source.domain.contest.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * Contest 목록 조회 응답 DTO
 */
public record ContestListResponse(
    PageData<ContestDetailResponse> data
) {
    /**
     * PageData로부터 ContestListResponse 생성
     * 
     * @param pageData PageData<ContestDetailResponse>
     * @return ContestListResponse
     */
    public static ContestListResponse from(PageData<ContestDetailResponse> pageData) {
        return new ContestListResponse(pageData);
    }
}
