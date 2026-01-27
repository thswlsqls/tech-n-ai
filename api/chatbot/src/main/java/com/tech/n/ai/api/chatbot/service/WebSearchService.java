package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.WebSearchDocument;

import java.util.List;

/**
 * Web 검색 서비스 인터페이스
 */
public interface WebSearchService {

    /**
     * 쿼리로 Web 검색 수행
     */
    List<WebSearchDocument> search(String query);

    /**
     * 쿼리로 Web 검색 수행 (결과 수 제한)
     */
    List<WebSearchDocument> search(String query, int maxResults);
}
