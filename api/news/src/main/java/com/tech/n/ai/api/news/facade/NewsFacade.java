package com.tech.n.ai.api.news.facade;

import com.tech.n.ai.api.news.dto.request.NewsBatchRequest;
import com.tech.n.ai.api.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.api.news.dto.request.NewsListRequest;
import com.tech.n.ai.api.news.dto.request.NewsSearchRequest;
import com.tech.n.ai.api.news.dto.response.NewsBatchResponse;
import com.tech.n.ai.api.news.dto.response.NewsDetailResponse;
import com.tech.n.ai.api.news.dto.response.NewsListResponse;
import com.tech.n.ai.api.news.dto.response.NewsSearchResponse;
import com.tech.n.ai.api.news.service.NewsService;
import com.tech.n.ai.common.core.dto.PageData;
import com.tech.n.ai.datasource.mongodb.document.NewsArticleDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * News Facade
 * Controller와 Service 사이의 중간 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFacade {
    
    private final NewsService newsService;
    
    /**
     * News 목록 조회
     * 
     * @param request NewsListRequest
     * @return NewsListResponse
     */
    public NewsListResponse getNewsList(NewsListRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            parseSort(request.sort())
        );
        
        ObjectId sourceId = request.sourceId() != null ? new ObjectId(request.sourceId()) : null;
        
        Page<NewsArticleDocument> page = newsService.findNewsArticles(
            sourceId,
            pageable
        );
        
        // Page<NewsArticleDocument>를 PageData<NewsDetailResponse>로 변환
        List<NewsDetailResponse> list = page.getContent().stream()
            .map(NewsDetailResponse::from)
            .toList();
        
        PageData<NewsDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return NewsListResponse.from(pageData);
    }
    
    /**
     * News 상세 조회
     * 
     * @param id News ID
     * @return NewsDetailResponse
     */
    public NewsDetailResponse getNewsDetail(String id) {
        NewsArticleDocument document = newsService.findNewsArticleById(id);
        return NewsDetailResponse.from(document);
    }
    
    /**
     * News 검색
     * 
     * @param request NewsSearchRequest
     * @return NewsSearchResponse
     */
    public NewsSearchResponse searchNews(NewsSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size()
        );
        
        Page<NewsArticleDocument> page = newsService.searchNewsArticles(
            request.q(),
            pageable
        );
        
        // Page<NewsArticleDocument>를 PageData<NewsDetailResponse>로 변환
        List<NewsDetailResponse> list = page.getContent().stream()
            .map(NewsDetailResponse::from)
            .toList();
        
        PageData<NewsDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return NewsSearchResponse.from(pageData);
    }
    
    /**
     * News 생성 (단건 처리, 내부 API)
     * 
     * @param request NewsCreateRequest
     * @return NewsDetailResponse
     */
    public NewsDetailResponse createNews(NewsCreateRequest request) {
        NewsArticleDocument document = newsService.saveNews(request);
        return NewsDetailResponse.from(document);
    }
    
    /**
     * News 다건 생성 (내부 API) - 부분 롤백 구현
     * @Transactional 없음 - 각 단건 처리가 독립적인 트랜잭션
     * 
     * @param request NewsBatchRequest
     * @return NewsBatchResponse
     */
    public NewsBatchResponse createNewsBatch(NewsBatchRequest request) {
        int successCount = 0;
        int failureCount = 0;
        List<String> failureMessages = new ArrayList<>();
        
        for (NewsCreateRequest item : request.newsArticles()) {
            try {
                // 단건 처리 Service 메서드 호출 (각 호출마다 독립적인 트랜잭션)
                newsService.saveNews(item);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String errorMessage = String.format(
                    "News 저장 실패: sourceId=%s, title=%s, error=%s",
                    item.sourceId(), item.title(), e.getMessage()
                );
                log.error(errorMessage, e);
                failureMessages.add(errorMessage);
                // 예외를 catch하고 로그만 출력하여 다음 항목 계속 처리
            }
        }
        
        return NewsBatchResponse.builder()
            .totalCount(request.newsArticles().size())
            .successCount(successCount)
            .failureCount(failureCount)
            .failureMessages(failureMessages)
            .build();
    }
    
    /**
     * 정렬 문자열을 Sort 객체로 변환
     * 
     * @param sort 정렬 문자열 (예: "publishedAt,desc")
     * @return Sort 객체
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }
        
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }
        
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();
        
        Sort.Direction sortDirection = "asc".equals(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(sortDirection, field);
    }
}
