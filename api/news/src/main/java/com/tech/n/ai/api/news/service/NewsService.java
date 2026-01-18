package com.tech.n.ai.api.news.service;

import com.tech.n.ai.api.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.datasource.mongodb.document.NewsArticleDocument;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * News Service 인터페이스
 */
public interface NewsService {
    
    /**
     * News 목록 조회
     * 
     * @param sourceId 출처 ID (optional)
     * @param pageable 페이징 정보
     * @return News 목록
     */
    Page<NewsArticleDocument> findNewsArticles(ObjectId sourceId, Pageable pageable);
    
    /**
     * News 상세 조회
     * 
     * @param id News ID
     * @return NewsArticleDocument
     */
    NewsArticleDocument findNewsArticleById(String id);
    
    /**
     * News 검색
     * 
     * @param query 검색어
     * @param pageable 페이징 정보
     * @return News 목록
     */
    Page<NewsArticleDocument> searchNewsArticles(String query, Pageable pageable);
    
    /**
     * News 저장 (단건 처리)
     * 
     * @param request News 생성 요청
     * @return 저장된 NewsArticleDocument
     */
    NewsArticleDocument saveNews(NewsCreateRequest request);
}
