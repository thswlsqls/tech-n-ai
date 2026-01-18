package com.tech.n.ai.client.feign.domain.internal.api;

import com.tech.n.ai.api.news.dto.response.NewsBatchResponse;
import com.tech.n.ai.api.news.dto.response.NewsDetailResponse;
import com.tech.n.ai.client.feign.domain.internal.client.NewsInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * News 내부 API Api 클래스
 * NewsInternalContract 인터페이스 구현
 */
@RequiredArgsConstructor
public class NewsInternalApi implements NewsInternalContract {
    
    private final NewsInternalFeignClient feignClient;
    
    @Override
    public ApiResponse<NewsDetailResponse> createNewsInternal(
            String apiKey,
            InternalApiDto.NewsCreateRequest request) {
        return feignClient.createNewsInternal(apiKey, request);
    }
    
    @Override
    public ApiResponse<NewsBatchResponse> createNewsBatchInternal(
            String apiKey,
            InternalApiDto.NewsBatchRequest request) {
        return feignClient.createNewsBatchInternal(apiKey, request);
    }
}
