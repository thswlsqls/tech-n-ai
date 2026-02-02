package com.tech.n.ai.api.bookmark.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * 북마크 히스토리 목록 조회 응답 DTO
 */
public record BookmarkHistoryListResponse(
    PageData<BookmarkHistoryDetailResponse> data
) {
    /**
     * PageData로부터 BookmarkHistoryListResponse 생성
     * 
     * @param pageData PageData<BookmarkHistoryDetailResponse>
     * @return BookmarkHistoryListResponse
     */
    public static BookmarkHistoryListResponse from(PageData<BookmarkHistoryDetailResponse> pageData) {
        return new BookmarkHistoryListResponse(pageData);
    }
}
