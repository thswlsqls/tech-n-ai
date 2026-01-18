package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;

import java.util.List;

/**
 * 프롬프트 서비스 인터페이스
 */
public interface PromptService {
    
    /**
     * 프롬프트 생성 (검색 결과 포함)
     * 
     * @param query 검색 쿼리
     * @param searchResults 검색 결과 목록
     * @return 생성된 프롬프트
     */
    String buildPrompt(String query, List<SearchResult> searchResults);
}
