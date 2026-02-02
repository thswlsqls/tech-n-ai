package com.tech.n.ai.api.archive.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * 아카이브 목록 조회 응답 DTO
 */
public record ArchiveListResponse(
    PageData<ArchiveDetailResponse> data
) {
    /**
     * PageData로부터 ArchiveListResponse 생성
     * 
     * @param pageData PageData<ArchiveDetailResponse>
     * @return ArchiveListResponse
     */
    public static ArchiveListResponse from(PageData<ArchiveDetailResponse> pageData) {
        return new ArchiveListResponse(pageData);
    }
}
