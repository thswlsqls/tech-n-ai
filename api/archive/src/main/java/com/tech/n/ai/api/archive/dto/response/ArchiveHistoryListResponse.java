package com.tech.n.ai.api.archive.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * 아카이브 히스토리 목록 조회 응답 DTO
 */
public record ArchiveHistoryListResponse(
    PageData<ArchiveHistoryDetailResponse> data
) {
    /**
     * PageData로부터 ArchiveHistoryListResponse 생성
     * 
     * @param pageData PageData<ArchiveHistoryDetailResponse>
     * @return ArchiveHistoryListResponse
     */
    public static ArchiveHistoryListResponse from(PageData<ArchiveHistoryDetailResponse> pageData) {
        return new ArchiveHistoryListResponse(pageData);
    }
}
