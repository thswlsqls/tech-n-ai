package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchOptions;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;

import java.util.List;

/**
 * 벡터 검색 서비스 인터페이스
 */
public interface VectorSearchService {
    
    /**
     * 벡터 검색 수행
     * 
     * @param query 검색 쿼리
     * @param userId JWT에서 추출한 사용자 ID (아카이브 검색 필터링용)
     * @param options 검색 옵션
     * @return 검색 결과 목록
     */
    List<SearchResult> search(String query, Long userId, SearchOptions options);
}
