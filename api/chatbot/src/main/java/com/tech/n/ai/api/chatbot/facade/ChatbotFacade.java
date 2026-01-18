package com.tech.n.ai.api.chatbot.facade;

import com.tech.n.ai.api.chatbot.dto.request.ChatRequest;
import com.tech.n.ai.api.chatbot.dto.response.ChatResponse;
import com.tech.n.ai.api.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 챗봇 Facade
 * Controller와 Service 사이의 중간 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotFacade {
    
    private final ChatbotService chatbotService;
    
    /**
     * 챗봇 응답 생성
     * 
     * @param request 챗봇 요청
     * @param userId JWT에서 추출한 사용자 ID
     * @return 챗봇 응답
     */
    public ChatResponse chat(ChatRequest request, Long userId) {
        return chatbotService.generateResponse(request, userId);
    }
}
