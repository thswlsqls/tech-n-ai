package com.tech.n.ai.client.feign.domain.internal.api;

import com.tech.n.ai.client.feign.domain.internal.client.EmergingTechInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * Emerging Tech 내부 API 구현체
 */
@RequiredArgsConstructor
public class EmergingTechInternalApi implements EmergingTechInternalContract {

    private final EmergingTechInternalFeignClient feignClient;

    @Override
    public ApiResponse<InternalApiDto.EmergingTechDetailResponse> createEmergingTechInternal(
            String apiKey,
            InternalApiDto.EmergingTechCreateRequest request) {
        return feignClient.createEmergingTechInternal(apiKey, request);
    }

    @Override
    public ApiResponse<InternalApiDto.EmergingTechBatchResponse> createEmergingTechBatchInternal(
            String apiKey,
            InternalApiDto.EmergingTechBatchRequest request) {
        return feignClient.createEmergingTechBatchInternal(apiKey, request);
    }

    @Override
    public ApiResponse<Object> searchEmergingTech(
            String apiKey,
            String query,
            String provider,
            int page,
            int size) {
        return feignClient.searchEmergingTech(apiKey, query, provider, page, size);
    }

    @Override
    public ApiResponse<Object> listEmergingTechs(
            String apiKey,
            String provider,
            String updateType,
            String status,
            String sourceType,
            String startDate,
            String endDate,
            int page,
            int size,
            String sort) {
        return feignClient.listEmergingTechs(apiKey, provider, updateType, status,
                sourceType, startDate, endDate, page, size, sort);
    }

    @Override
    public ApiResponse<Object> getEmergingTechDetail(
            String apiKey,
            String id) {
        return feignClient.getEmergingTechDetail(apiKey, id);
    }

    @Override
    public ApiResponse<Object> approveEmergingTech(
            String apiKey,
            String id) {
        return feignClient.approveEmergingTech(apiKey, id);
    }
}
