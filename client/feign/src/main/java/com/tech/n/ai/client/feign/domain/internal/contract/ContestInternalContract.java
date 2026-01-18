package com.tech.n.ai.client.feign.domain.internal.contract;

import com.tech.n.ai.api.contest.dto.response.ContestBatchResponse;
import com.tech.n.ai.api.contest.dto.response.ContestDetailResponse;
import com.tech.n.ai.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Contest 내부 API Contract 인터페이스
 * Batch 모듈에서 api-contest 모듈의 내부 API를 호출하기 위한 인터페이스
 */
public interface ContestInternalContract {
    
    /**
     * Contest 단건 생성 (내부 API)
     * 
     * @param apiKey 내부 API 키
     * @param request Contest 생성 요청 DTO
     * @return Contest 상세 응답
     */
    @PostMapping("/api/v1/contest/internal")
    ApiResponse<ContestDetailResponse> createContestInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.ContestCreateRequest request);
    
    /**
     * Contest 다건 생성 (내부 API)
     * 
     * @param apiKey 내부 API 키
     * @param request Contest 다건 생성 요청 DTO
     * @return Contest 배치 응답
     */
    @PostMapping("/api/v1/contest/internal/batch")
    ApiResponse<ContestBatchResponse> createContestBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.ContestBatchRequest request);
}
