package com.tech.n.ai.client.feign.domain.internal.contract;

import com.tech.n.ai.api.news.dto.response.NewsBatchResponse;
import com.tech.n.ai.api.news.dto.response.NewsDetailResponse;
import com.tech.n.ai.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * News 내부 API Contract 인터페이스
 * Batch 모듈에서 api-news 모듈의 내부 API를 호출하기 위한 인터페이스
 */
public interface NewsInternalContract {
    
    /**
     * News 단건 생성 (내부 API)
     * 
     * @param apiKey 내부 API 키
     * @param request News 생성 요청 DTO
     * @return News 상세 응답
     */
    @PostMapping("/api/v1/news/internal")
    ApiResponse<NewsDetailResponse> createNewsInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.NewsCreateRequest request);
    
    /**
     * News 다건 생성 (내부 API)
     * 
     * @param apiKey 내부 API 키
     * @param request News 다건 생성 요청 DTO
     * @return News 배치 응답
     */
    @PostMapping("/api/v1/news/internal/batch")
    ApiResponse<NewsBatchResponse> createNewsBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.NewsBatchRequest request);
}
