package com.tech.n.ai.api.bookmark.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * 북마크 검색 응답 DTO
 */
public record BookmarkSearchResponse(
    PageData<BookmarkDetailResponse> data
) {
    /**
     * PageData로부터 BookmarkSearchResponse 생성
     * 
     * @param pageData PageData<BookmarkDetailResponse>
     * @return BookmarkSearchResponse
     */
    public static BookmarkSearchResponse from(PageData<BookmarkDetailResponse> pageData) {
        return new BookmarkSearchResponse(pageData);
    }
}
