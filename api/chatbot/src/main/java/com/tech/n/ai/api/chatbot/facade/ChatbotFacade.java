package com.tech.n.ai.api.chatbot.facade;

import com.tech.n.ai.api.chatbot.dto.request.ChatRequest;
import com.tech.n.ai.api.chatbot.dto.response.ChatResponse;
import com.tech.n.ai.api.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotFacade {
    
    private final ChatbotService chatbotService;
    
    public ChatResponse chat(ChatRequest request, Long userId, String userRole) {
        return chatbotService.generateResponse(request, userId, userRole);
    }
}
