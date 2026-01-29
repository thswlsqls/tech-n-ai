package com.tech.n.ai.client.feign.domain.internal.contract;

import com.ebson.shrimp.tm.demo.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Emerging Tech 내부 API Contract 인터페이스
 * Batch 모듈 및 AI Agent에서 api-emerging-tech 모듈의 내부 API 호출용
 */
public interface EmergingTechInternalContract {

    /**
     * Emerging Tech 단건 생성 (내부 API)
     */
    @PostMapping("/api/v1/emerging-tech/internal")
    ApiResponse<Object> createEmergingTechInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.EmergingTechCreateRequest request);

    /**
     * Emerging Tech 다건 생성 (내부 API)
     */
    @PostMapping("/api/v1/emerging-tech/internal/batch")
    ApiResponse<Object> createEmergingTechBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.EmergingTechBatchRequest request);

    /**
     * Emerging Tech 검색
     */
    @GetMapping("/api/v1/emerging-tech/search")
    ApiResponse<Object> searchEmergingTech(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestParam("query") String query,
        @RequestParam(value = "provider", required = false) String provider,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size);

    /**
     * Emerging Tech 승인 (내부 API)
     */
    @PostMapping("/api/v1/emerging-tech/{id}/approve")
    ApiResponse<Object> approveEmergingTech(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @PathVariable("id") String id);
}
