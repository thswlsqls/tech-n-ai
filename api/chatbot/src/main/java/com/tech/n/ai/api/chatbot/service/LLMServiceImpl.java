package com.tech.n.ai.api.chatbot.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * LLM 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {
    
    private final ChatLanguageModel chatLanguageModel;
    
    @Override
    public String generate(String prompt) {
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            log.error("Failed to generate LLM response", e);
            throw new RuntimeException("LLM 응답 생성 실패", e);
        }
    }
}
