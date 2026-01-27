package com.tech.n.ai.api.aiupdate.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * AI Update 다건 생성 요청 DTO
 */
public record AiUpdateBatchRequest(
    @NotEmpty @Valid List<AiUpdateCreateRequest> items
) {}
