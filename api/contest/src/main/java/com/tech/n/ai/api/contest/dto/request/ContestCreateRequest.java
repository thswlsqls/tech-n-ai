package com.tech.n.ai.api.contest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Contest 생성 요청 DTO (내부 API용)
 */
public record ContestCreateRequest(
    @NotBlank(message = "sourceId는 필수입니다.")
    String sourceId,
    
    @NotBlank(message = "제목은 필수입니다.")
    String title,
    
    @NotNull(message = "시작일은 필수입니다.")
    LocalDateTime startDate,
    
    @NotNull(message = "종료일은 필수입니다.")
    LocalDateTime endDate,
    
    String description,
    
    @NotBlank(message = "URL은 필수입니다.")
    String url,
    
    @Valid
    ContestMetadataRequest metadata
) {
    /**
     * ContestMetadata 요청 DTO
     */
    public record ContestMetadataRequest(
        String sourceName,
        String prize,
        Integer participants,
        java.util.List<String> tags
    ) {
    }
}
