package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.Intent;

/**
 * 의도 분류 서비스 인터페이스
 */
public interface IntentClassificationService {
    
    /**
     * 의도 분류
     * 
     * @param preprocessedInput 전처리된 입력
     * @return 의도 분류 결과
     */
    Intent classifyIntent(String preprocessedInput);
}
