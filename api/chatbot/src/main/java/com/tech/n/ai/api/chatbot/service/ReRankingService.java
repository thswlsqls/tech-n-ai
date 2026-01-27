package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;

import java.util.List;

/**
 * Re-Ranking 서비스 인터페이스
 */
public interface ReRankingService {

    /**
     * 검색 결과를 쿼리와의 관련성 기준으로 재정렬
     */
    List<SearchResult> rerank(String query, List<SearchResult> documents, int topK);

    /**
     * Re-Ranking 서비스 활성화 여부
     */
    boolean isEnabled();
}
