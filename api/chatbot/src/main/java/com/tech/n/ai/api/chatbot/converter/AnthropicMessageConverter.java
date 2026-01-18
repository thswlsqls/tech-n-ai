package com.tech.n.ai.api.chatbot.converter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Anthropic 메시지 포맷 변환기 (대안)
 */
@Slf4j
@Component("anthropicMessageConverter")
public class AnthropicMessageConverter implements MessageFormatConverter {
    
    @Override
    public Object convertToProviderFormat(List<ChatMessage> messages, String systemPrompt) {
        // SystemMessage를 system 파라미터로 추출
        String systemParam = messages.stream()
            .filter(m -> m instanceof SystemMessage)
            .map(m -> ((SystemMessage) m).text())
            .findFirst()
            .orElse(systemPrompt != null ? systemPrompt : "");
        
        // UserMessage와 AiMessage만 messages 배열에 포함
        List<Map<String, String>> anthropicMessages = messages.stream()
            .filter(m -> m instanceof UserMessage || m instanceof AiMessage)
            .map(m -> {
                Map<String, String> message = new HashMap<>();
                if (m instanceof UserMessage) {
                    message.put("role", "user");
                    message.put("content", ((UserMessage) m).singleText());
                } else if (m instanceof AiMessage) {
                    message.put("role", "assistant");
                    message.put("content", ((AiMessage) m).text());
                }
                return message;
            })
            .collect(Collectors.toList());
        
        // Anthropic API 포맷: system 파라미터와 messages 배열을 별도로 반환
        Map<String, Object> anthropicFormat = new HashMap<>();
        anthropicFormat.put("system", systemParam);
        anthropicFormat.put("messages", anthropicMessages);
        
        return anthropicFormat;
    }
    
    @Override
    public List<ChatMessage> convertFromProviderFormat(Object providerResponse) {
        // Anthropic 응답을 ChatMessage로 변환
        // 실제 구현에서는 Anthropic API 응답 구조에 맞게 변환
        return new ArrayList<>();
    }
}
