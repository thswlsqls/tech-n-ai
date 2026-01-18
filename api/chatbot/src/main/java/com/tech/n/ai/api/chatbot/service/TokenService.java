package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import com.tech.n.ai.api.chatbot.service.dto.TokenUsage;

import java.util.List;

/**
 * 토큰 서비스 인터페이스
 */
public interface TokenService {
    
    /**
     * 토큰 수 예측
     * 
     * @param text 텍스트
     * @return 예상 토큰 수
     */
    int estimateTokens(String text);
    
    /**
     * 입력 토큰 검증
     * 
     * @param prompt 프롬프트
     */
    void validateInputTokens(String prompt);
    
    /**
     * 검색 결과 토큰 제한
     * 
     * @param results 검색 결과 목록
     * @param maxTokens 최대 토큰 수
     * @return 제한된 검색 결과 목록
     */
    List<SearchResult> truncateResults(List<SearchResult> results, int maxTokens);
    
    /**
     * 토큰 사용량 추적
     * 
     * @param requestId 요청 ID
     * @param userId 사용자 ID
     * @param inputTokens 입력 토큰 수
     * @param outputTokens 출력 토큰 수
     * @return 토큰 사용량 정보
     */
    TokenUsage trackUsage(String requestId, String userId, int inputTokens, int outputTokens);
}
