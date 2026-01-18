package com.tech.n.ai.api.contest.controller;

import com.tech.n.ai.api.contest.config.ContestConfig;
import com.tech.n.ai.api.contest.dto.request.ContestBatchRequest;
import com.tech.n.ai.api.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.api.contest.dto.request.ContestListRequest;
import com.tech.n.ai.api.contest.dto.request.ContestSearchRequest;
import com.tech.n.ai.api.contest.dto.response.ContestBatchResponse;
import com.tech.n.ai.api.contest.dto.response.ContestDetailResponse;
import com.tech.n.ai.api.contest.dto.response.ContestListResponse;
import com.tech.n.ai.api.contest.dto.response.ContestSearchResponse;
import com.tech.n.ai.api.contest.facade.ContestFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contest API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/contest")
@RequiredArgsConstructor
public class ContestController {
    
    private final ContestFacade contestFacade;
    private final ContestConfig contestConfig;
    
    /**
     * Contest 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ContestListResponse>> getContestList(
            @Valid ContestListRequest request) {
        ContestListResponse response = contestFacade.getContestList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Contest 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContestDetailResponse>> getContestDetail(
            @PathVariable String id) {
        ContestDetailResponse response = contestFacade.getContestDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Contest 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ContestSearchResponse>> searchContest(
            @Valid ContestSearchRequest request) {
        ContestSearchResponse response = contestFacade.searchContest(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Contest 생성 (내부 API, Batch 모듈 전용)
     */
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<ContestDetailResponse>> createContestInternal(
            @Valid @RequestBody ContestCreateRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        // 내부 API 키 검증
        validateInternalApiKey(apiKey);
        
        ContestDetailResponse response = contestFacade.createContest(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Contest 다건 생성 (내부 API, Batch 모듈 전용)
     */
    @PostMapping("/internal/batch")
    public ResponseEntity<ApiResponse<ContestBatchResponse>> createContestBatchInternal(
            @Valid @RequestBody ContestBatchRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        // 내부 API 키 검증
        validateInternalApiKey(apiKey);
        
        ContestBatchResponse response = contestFacade.createContestBatch(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 내부 API 키 검증
     * 
     * @param apiKey 요청 헤더의 API 키
     * @throws UnauthorizedException API 키가 유효하지 않은 경우
     */
    private void validateInternalApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("내부 API 키가 제공되지 않았습니다.");
        }
        
        if (contestConfig.getApiKey() == null || contestConfig.getApiKey().isBlank()) {
            log.warn("내부 API 키가 설정되지 않았습니다. 설정 파일을 확인하세요.");
            throw new UnauthorizedException("내부 API 키가 설정되지 않았습니다.");
        }
        
        if (!contestConfig.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("유효하지 않은 내부 API 키입니다.");
        }
    }
}
