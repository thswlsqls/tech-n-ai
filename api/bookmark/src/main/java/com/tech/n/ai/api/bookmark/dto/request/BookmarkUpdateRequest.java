package com.tech.n.ai.api.bookmark.dto.request;

/**
 * 북마크 수정 요청 DTO
 */
public record BookmarkUpdateRequest(
    String tag,
    String memo
) {
}
