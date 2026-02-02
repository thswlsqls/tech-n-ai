package com.tech.n.ai.api.bookmark.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * 북마크 목록 조회 응답 DTO
 */
public record BookmarkListResponse(
    PageData<BookmarkDetailResponse> data
) {
    /**
     * PageData로부터 BookmarkListResponse 생성
     * 
     * @param pageData PageData<BookmarkDetailResponse>
     * @return BookmarkListResponse
     */
    public static BookmarkListResponse from(PageData<BookmarkDetailResponse> pageData) {
        return new BookmarkListResponse(pageData);
    }
}
