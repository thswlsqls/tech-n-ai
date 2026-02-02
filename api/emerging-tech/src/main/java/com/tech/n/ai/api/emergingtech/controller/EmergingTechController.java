package com.tech.n.ai.api.emergingtech.controller;

import com.tech.n.ai.api.emergingtech.config.EmergingTechConfig;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechBatchRequest;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechListRequest;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechSearchRequest;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechBatchResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechDetailResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechListResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechSearchResponse;
import com.tech.n.ai.api.emergingtech.facade.EmergingTechFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Emerging Tech API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/emerging-tech")
@RequiredArgsConstructor
public class EmergingTechController {

    private final EmergingTechFacade emergingTechFacade;
    private final EmergingTechConfig emergingTechConfig;

    /**
     * Emerging Tech 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<EmergingTechListResponse>> getEmergingTechList(
            @Valid EmergingTechListRequest request) {
        EmergingTechListResponse response = emergingTechFacade.getEmergingTechList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Emerging Tech 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmergingTechDetailResponse>> getEmergingTechDetail(
            @PathVariable String id) {
        EmergingTechDetailResponse response = emergingTechFacade.getEmergingTechDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Emerging Tech 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<EmergingTechSearchResponse>> searchEmergingTech(
            @Valid EmergingTechSearchRequest request) {
        EmergingTechSearchResponse response = emergingTechFacade.searchEmergingTech(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Emerging Tech 생성 (내부 API)
     */
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<EmergingTechDetailResponse>> createEmergingTechInternal(
            @Valid @RequestBody EmergingTechCreateRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        EmergingTechDetailResponse response = emergingTechFacade.createEmergingTech(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Emerging Tech 다건 생성 (내부 API)
     */
    @PostMapping("/internal/batch")
    public ResponseEntity<ApiResponse<EmergingTechBatchResponse>> createEmergingTechBatchInternal(
            @Valid @RequestBody EmergingTechBatchRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        EmergingTechBatchResponse response = emergingTechFacade.createEmergingTechBatch(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Emerging Tech 승인 (내부 API)
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<EmergingTechDetailResponse>> approveEmergingTech(
            @PathVariable String id,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        EmergingTechDetailResponse response = emergingTechFacade.approveEmergingTech(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Emerging Tech 거부 (내부 API)
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<EmergingTechDetailResponse>> rejectEmergingTech(
            @PathVariable String id,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        EmergingTechDetailResponse response = emergingTechFacade.rejectEmergingTech(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내부 API 키 검증
     */
    private void validateInternalApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("내부 API 키가 제공되지 않았습니다.");
        }

        if (emergingTechConfig.getApiKey() == null || emergingTechConfig.getApiKey().isBlank()) {
            log.warn("내부 API 키가 설정되지 않았습니다. 설정 파일을 확인하세요.");
            throw new UnauthorizedException("내부 API 키가 설정되지 않았습니다.");
        }

        if (!emergingTechConfig.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("유효하지 않은 내부 API 키입니다.");
        }
    }
}
