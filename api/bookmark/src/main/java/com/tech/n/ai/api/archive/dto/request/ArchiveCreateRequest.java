package com.tech.n.ai.api.archive.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 아카이브 생성 요청 DTO
 */
public record ArchiveCreateRequest(
    @NotBlank(message = "항목 타입은 필수입니다.")
    String itemType,
    
    @NotBlank(message = "항목 ID는 필수입니다.")
    String itemId,
    
    String tag,
    String memo
) {
}
