package com.tech.n.ai.api.bookmark.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 북마크 목록 조회 요청 DTO
 */
public record BookmarkListRequest(
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    Integer page,
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    Integer size,
    
    String sort,
    String itemType
) {
    public BookmarkListRequest {
        if (page == null) {
            page = 1;
        }
        if (size == null) {
            size = 10;
        }
        if (sort == null) {
            sort = "bookmarkedAt,desc";
        }
    }
}
