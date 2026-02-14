package com.tech.n.ai.api.emergingtech.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * Emerging Tech 다건 생성 응답 DTO
 */
@Builder
public record EmergingTechBatchResponse(
    int totalCount,
    int successCount,
    int newCount,
    int duplicateCount,
    int failureCount,
    List<String> failureMessages
) {}
