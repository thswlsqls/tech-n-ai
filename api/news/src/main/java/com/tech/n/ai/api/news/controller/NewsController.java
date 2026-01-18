package com.tech.n.ai.api.news.controller;

import com.tech.n.ai.api.news.config.NewsConfig;
import com.tech.n.ai.api.news.dto.request.NewsBatchRequest;
import com.tech.n.ai.api.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.api.news.dto.request.NewsListRequest;
import com.tech.n.ai.api.news.dto.request.NewsSearchRequest;
import com.tech.n.ai.api.news.dto.response.NewsBatchResponse;
import com.tech.n.ai.api.news.dto.response.NewsDetailResponse;
import com.tech.n.ai.api.news.dto.response.NewsListResponse;
import com.tech.n.ai.api.news.dto.response.NewsSearchResponse;
import com.tech.n.ai.api.news.facade.NewsFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * News API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {
    
    private final NewsFacade newsFacade;
    private final NewsConfig newsConfig;
    
    /**
     * News 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<NewsListResponse>> getNewsList(
            @Valid NewsListRequest request) {
        NewsListResponse response = newsFacade.getNewsList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * News 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNewsDetail(
            @PathVariable String id) {
        NewsDetailResponse response = newsFacade.getNewsDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * News 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<NewsSearchResponse>> searchNews(
            @Valid NewsSearchRequest request) {
        NewsSearchResponse response = newsFacade.searchNews(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * News 생성 (내부 API, Batch 모듈 전용)
     */
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> createNewsInternal(
            @Valid @RequestBody NewsCreateRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        // 내부 API 키 검증
        validateInternalApiKey(apiKey);
        
        NewsDetailResponse response = newsFacade.createNews(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * News 다건 생성 (내부 API, Batch 모듈 전용)
     */
    @PostMapping("/internal/batch")
    public ResponseEntity<ApiResponse<NewsBatchResponse>> createNewsBatchInternal(
            @Valid @RequestBody NewsBatchRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        // 내부 API 키 검증
        validateInternalApiKey(apiKey);
        
        NewsBatchResponse response = newsFacade.createNewsBatch(request);
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
        
        if (newsConfig.getApiKey() == null || newsConfig.getApiKey().isBlank()) {
            log.warn("내부 API 키가 설정되지 않았습니다. 설정 파일을 확인하세요.");
            throw new UnauthorizedException("내부 API 키가 설정되지 않았습니다.");
        }
        
        if (!newsConfig.getApiKey().equals(apiKey)) {
            throw new UnauthorizedException("유효하지 않은 내부 API 키입니다.");
        }
    }
}
