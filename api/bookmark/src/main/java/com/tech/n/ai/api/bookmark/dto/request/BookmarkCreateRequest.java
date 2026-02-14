package com.tech.n.ai.api.bookmark.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 북마크 생성 요청 DTO
 */
public record BookmarkCreateRequest(
    @NotBlank(message = "EmergingTech ID는 필수입니다.")
    String emergingTechId,

    List<String> tags,
    String memo
) {
}
