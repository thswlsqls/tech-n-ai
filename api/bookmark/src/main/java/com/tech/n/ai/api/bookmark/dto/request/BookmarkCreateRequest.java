package com.tech.n.ai.api.bookmark.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 북마크 생성 요청 DTO
 */
public record BookmarkCreateRequest(
    @NotBlank(message = "항목 타입은 필수입니다.")
    String itemType,
    
    @NotBlank(message = "항목 ID는 필수입니다.")
    String itemId,
    
    String tag,
    String memo
) {
}
