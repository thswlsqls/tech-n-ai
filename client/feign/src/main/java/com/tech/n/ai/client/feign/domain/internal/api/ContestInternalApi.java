package com.tech.n.ai.client.feign.domain.internal.api;

import com.tech.n.ai.api.contest.dto.response.ContestBatchResponse;
import com.tech.n.ai.api.contest.dto.response.ContestDetailResponse;
import com.tech.n.ai.client.feign.domain.internal.client.ContestInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * Contest 내부 API Api 클래스
 * ContestInternalContract 인터페이스 구현
 */
@RequiredArgsConstructor
public class ContestInternalApi implements ContestInternalContract {
    
    private final ContestInternalFeignClient feignClient;
    
    @Override
    public ApiResponse<ContestDetailResponse> createContestInternal(
            String apiKey,
            InternalApiDto.ContestCreateRequest request) {
        return feignClient.createContestInternal(apiKey, request);
    }
    
    @Override
    public ApiResponse<ContestBatchResponse> createContestBatchInternal(
            String apiKey,
            InternalApiDto.ContestBatchRequest request) {
        return feignClient.createContestBatchInternal(apiKey, request);
    }
}
