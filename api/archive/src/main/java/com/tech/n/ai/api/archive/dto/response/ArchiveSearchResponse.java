package com.tech.n.ai.api.archive.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * 아카이브 검색 응답 DTO
 */
public record ArchiveSearchResponse(
    PageData<ArchiveDetailResponse> data
) {
    /**
     * PageData로부터 ArchiveSearchResponse 생성
     * 
     * @param pageData PageData<ArchiveDetailResponse>
     * @return ArchiveSearchResponse
     */
    public static ArchiveSearchResponse from(PageData<ArchiveDetailResponse> pageData) {
        return new ArchiveSearchResponse(pageData);
    }
}
