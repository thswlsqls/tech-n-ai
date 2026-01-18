package com.tech.n.ai.api.chatbot.converter;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * Provider별 메시지 포맷 변환 인터페이스
 */
public interface MessageFormatConverter {
    
    /**
     * ChatMessage 리스트를 Provider 포맷으로 변환
     * 
     * @param messages ChatMessage 리스트
     * @param systemPrompt 시스템 프롬프트 (선택)
     * @return Provider별 요청 객체
     */
    Object convertToProviderFormat(List<ChatMessage> messages, String systemPrompt);
    
    /**
     * Provider 응답을 ChatMessage 리스트로 변환
     * 
     * @param providerResponse Provider 응답 객체
     * @return ChatMessage 리스트
     */
    List<ChatMessage> convertFromProviderFormat(Object providerResponse);
}
