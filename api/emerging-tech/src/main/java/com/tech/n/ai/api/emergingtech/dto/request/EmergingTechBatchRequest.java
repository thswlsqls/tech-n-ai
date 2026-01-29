package com.tech.n.ai.api.emergingtech.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Emerging Tech 다건 생성 요청 DTO
 */
public record EmergingTechBatchRequest(
    @NotEmpty @Valid List<EmergingTechCreateRequest> items
) {}
