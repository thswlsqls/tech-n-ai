package com.tech.n.ai.batch.source.domain.contest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

/**
 * Contest 다건 생성 요청 DTO (내부 API용)
 */
@Builder
public record ContestBatchRequest(
    @NotEmpty(message = "Contest 목록은 필수입니다.")
    @Valid
    List<ContestCreateRequest> contests
) {
}
