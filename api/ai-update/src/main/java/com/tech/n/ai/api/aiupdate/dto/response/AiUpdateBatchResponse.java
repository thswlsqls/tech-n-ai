package com.tech.n.ai.api.aiupdate.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * AI Update 다건 생성 응답 DTO
 */
@Builder
public record AiUpdateBatchResponse(
    int totalCount,
    int successCount,
    int failureCount,
    List<String> failureMessages
) {}
