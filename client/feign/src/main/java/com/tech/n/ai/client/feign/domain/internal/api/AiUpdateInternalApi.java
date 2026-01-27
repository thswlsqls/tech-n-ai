package com.tech.n.ai.client.feign.domain.internal.api;

import com.tech.n.ai.client.feign.domain.internal.client.AiUpdateInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.AiUpdateInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * AI Update 내부 API 구현체
 */
@RequiredArgsConstructor
public class AiUpdateInternalApi implements AiUpdateInternalContract {

    private final AiUpdateInternalFeignClient feignClient;

    @Override
    public ApiResponse<Object> createAiUpdateInternal(
            String apiKey,
            InternalApiDto.AiUpdateCreateRequest request) {
        return feignClient.createAiUpdateInternal(apiKey, request);
    }

    @Override
    public ApiResponse<Object> createAiUpdateBatchInternal(
            String apiKey,
            InternalApiDto.AiUpdateBatchRequest request) {
        return feignClient.createAiUpdateBatchInternal(apiKey, request);
    }

    @Override
    public ApiResponse<Object> searchAiUpdate(
            String apiKey,
            String query,
            String provider,
            int page,
            int size) {
        return feignClient.searchAiUpdate(apiKey, query, provider, page, size);
    }

    @Override
    public ApiResponse<Object> approveAiUpdate(
            String apiKey,
            String id) {
        return feignClient.approveAiUpdate(apiKey, id);
    }
}
