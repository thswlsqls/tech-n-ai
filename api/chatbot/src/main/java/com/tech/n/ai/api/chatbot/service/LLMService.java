package com.tech.n.ai.api.chatbot.service;

/**
 * LLM 서비스 인터페이스
 */
public interface LLMService {
    
    /**
     * LLM 응답 생성
     * 
     * @param prompt 프롬프트
     * @return LLM 응답
     */
    String generate(String prompt);
}
