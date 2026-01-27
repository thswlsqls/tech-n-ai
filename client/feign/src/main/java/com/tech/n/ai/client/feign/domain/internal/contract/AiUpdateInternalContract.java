package com.tech.n.ai.client.feign.domain.internal.contract;

import com.tech.n.ai.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

/**
 * AI Update 내부 API Contract 인터페이스
 * Batch 모듈 및 AI Agent에서 api-ai-update 모듈의 내부 API 호출용
 */
public interface AiUpdateInternalContract {

    /**
     * AI Update 단건 생성 (내부 API)
     */
    @PostMapping("/api/v1/ai-update/internal")
    ApiResponse<Object> createAiUpdateInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.AiUpdateCreateRequest request);

    /**
     * AI Update 다건 생성 (내부 API)
     */
    @PostMapping("/api/v1/ai-update/internal/batch")
    ApiResponse<Object> createAiUpdateBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.AiUpdateBatchRequest request);

    /**
     * AI Update 검색
     */
    @GetMapping("/api/v1/ai-update/search")
    ApiResponse<Object> searchAiUpdate(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestParam("query") String query,
        @RequestParam(value = "provider", required = false) String provider,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size);

    /**
     * AI Update 승인 (내부 API)
     */
    @PostMapping("/api/v1/ai-update/{id}/approve")
    ApiResponse<Object> approveAiUpdate(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @PathVariable("id") String id);
}
