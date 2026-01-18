package com.tech.n.ai.api.contest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Contest 다건 생성 요청 DTO (내부 API용)
 */
public record ContestBatchRequest(
    @NotEmpty(message = "Contest 목록은 필수입니다.")
    @Valid
    List<ContestCreateRequest> contests
) {
}
