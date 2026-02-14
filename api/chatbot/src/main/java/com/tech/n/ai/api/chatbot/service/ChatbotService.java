package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.dto.request.ChatRequest;
import com.tech.n.ai.api.chatbot.dto.response.ChatResponse;

/**
 * 챗봇 서비스 인터페이스
 */
public interface ChatbotService {
    
    /**
     * 챗봇 응답 생성
     * 
     * @param request 챗봇 요청
     * @param userId JWT에서 추출한 사용자 ID
     * @return 챗봇 응답
     */
    ChatResponse generateResponse(ChatRequest request, Long userId, String userRole);
}
