package com.tech.n.ai.api.bookmark.dto.request;

import java.util.List;

/**
 * 북마크 수정 요청 DTO
 */
public record BookmarkUpdateRequest(
    List<String> tags,
    String memo
) {
}
