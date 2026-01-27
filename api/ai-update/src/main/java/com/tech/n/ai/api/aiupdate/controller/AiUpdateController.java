package com.tech.n.ai.api.aiupdate.controller;

import com.tech.n.ai.api.aiupdate.config.AiUpdateConfig;
import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateBatchRequest;
import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateCreateRequest;
import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateListRequest;
import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateSearchRequest;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateBatchResponse;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateDetailResponse;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateListResponse;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateSearchResponse;
import com.tech.n.ai.api.aiupdate.facade.AiUpdateFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI Update API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai-update")
@RequiredArgsConstructor
public class AiUpdateController {

    private final AiUpdateFacade aiUpdateFacade;
    private final AiUpdateConfig aiUpdateConfig;

    /**
     * AI Update 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AiUpdateListResponse>> getAiUpdateList(
            @Valid AiUpdateListRequest request) {
        AiUpdateListResponse response = aiUpdateFacade.getAiUpdateList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AI Update 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AiUpdateDetailResponse>> getAiUpdateDetail(
            @PathVariable String id) {
        AiUpdateDetailResponse response = aiUpdateFacade.getAiUpdateDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AI Update 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<AiUpdateSearchResponse>> searchAiUpdate(
            @Valid AiUpdateSearchRequest request) {
        AiUpdateSearchResponse response = aiUpdateFacade.searchAiUpdate(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AI Update 생성 (내부 API)
     */
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<AiUpdateDetailResponse>> createAiUpdateInternal(
            @Valid @RequestBody AiUpdateCreateRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        AiUpdateDetailResponse response = aiUpdateFacade.createAiUpdate(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AI Update 다건 생성 (내부 API)
     */
    @PostMapping("/internal/batch")
    public ResponseEntity<ApiResponse<AiUpdateBatchResponse>> createAiUpdateBatchInternal(
            @Valid @RequestBody AiUpdateBatchRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        AiUpdateBatchResponse response = aiUpdateFacade.createAiUpdateBatch(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AI Update 승인 (내부 API)
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AiUpdateDetailResponse>> approveAiUpdate(
            @PathVariable String id,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        AiUpdateDetailResponse response = aiUpdateFacade.approveAiUpdate(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AI Update 거부 (내부 API)
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AiUpdateDetailResponse>> rejectAiUpdate(
            @PathVariable String id,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        validateInternalApiKey(apiKey);
        AiUpdateDetailResponse response = aiUpdateFacade.rejectAiUpdate(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내부 API 키 검증
     */
    private void validateInternalApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("내부 API 키가 제공되지 않았습니다.");
        }

        if (aiUpdateConfig.getApiKey() == null || aiUpdateConfig.getApiKey().isBlank()) {
            log.warn("내부 API 키가 설정되지 않았습니다. 설정 파일을 확인하세요.");
            throw new UnauthorizedException("내부 API 키가 설정되지 않았습니다.");
        }

        if (!aiUpdateConfig.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("유효하지 않은 내부 API 키입니다.");
        }
    }
}
