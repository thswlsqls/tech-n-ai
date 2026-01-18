package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.PreprocessedInput;

/**
 * 입력 전처리 서비스 인터페이스
 */
public interface InputPreprocessingService {
    
    /**
     * 입력 전처리
     * 
     * @param rawInput 원본 입력
     * @return 전처리된 입력
     */
    PreprocessedInput preprocess(String rawInput);
}
