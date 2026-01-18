package com.tech.n.ai.api.chatbot.converter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 메시지 포맷 변환기 (기본 선택)
 * 
 * 참고: OpenAI API 역할 변경사항 (2025)
 * - GPT-4o-mini: "system" 역할 지원 (현재 사용 중)
 * - O1 모델 이상: "developer" 역할 사용 필요 (향후 O1 모델 사용 시 수정 필요)
 * - 공식 문서: https://platform.openai.com/docs/guides/chat-completions
 */
@Slf4j
@Component("openAiMessageConverter")
public class OpenAiMessageConverter implements MessageFormatConverter {
    
    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o-mini}")
    private String modelName;
    
    @Override
    public Object convertToProviderFormat(List<ChatMessage> messages, String systemPrompt) {
        List<Map<String, String>> openAiMessages = new ArrayList<>();
        
        // SystemMessage는 messages 배열에 포함
        // 참고: GPT-4o-mini는 "system" 역할 지원, O1 모델 이상은 "developer" 역할 사용 필요
        String systemRole = modelName.startsWith("o1") ? "developer" : "system";
        
        for (ChatMessage message : messages) {
            Map<String, String> openAiMessage = new HashMap<>();
            
            if (message instanceof SystemMessage) {
                openAiMessage.put("role", systemRole);  // GPT-4o-mini: "system", O1: "developer"
                openAiMessage.put("content", ((SystemMessage) message).text());
            } else if (message instanceof UserMessage) {
                openAiMessage.put("role", "user");
                openAiMessage.put("content", ((UserMessage) message).singleText());
            } else if (message instanceof AiMessage) {
                openAiMessage.put("role", "assistant");
                openAiMessage.put("content", ((AiMessage) message).text());
            } else {
                log.warn("Unknown message type: {}", message.getClass().getName());
                continue;
            }
            
            openAiMessages.add(openAiMessage);
        }
        
        return openAiMessages;
    }
    
    @Override
    public List<ChatMessage> convertFromProviderFormat(Object providerResponse) {
        // OpenAI 응답을 ChatMessage로 변환
        // 실제 구현에서는 OpenAI API 응답 구조에 맞게 변환
        // 현재는 langchain4j가 자동으로 처리하므로 빈 리스트 반환
        return new ArrayList<>();
    }
}
