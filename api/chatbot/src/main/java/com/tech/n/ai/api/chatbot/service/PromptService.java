package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import com.tech.n.ai.api.chatbot.service.dto.WebSearchDocument;

import java.util.List;

/**
 * 프롬프트 서비스 인터페이스
 */
public interface PromptService {

    /**
     * RAG 프롬프트 생성 (검색 결과 포함)
     */
    String buildPrompt(String query, List<SearchResult> searchResults);

    /**
     * Web 검색 프롬프트 생성
     */
    String buildWebSearchPrompt(String query, List<WebSearchDocument> webResults);
}
