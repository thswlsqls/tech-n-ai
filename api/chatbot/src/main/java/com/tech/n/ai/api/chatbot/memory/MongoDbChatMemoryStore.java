package com.tech.n.ai.api.chatbot.memory;

import com.tech.n.ai.domain.mongodb.document.ConversationMessageDocument;
import com.tech.n.ai.domain.mongodb.repository.ConversationMessageRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB 기반 ChatMemoryStore 구현
 * 
 * TODO: langchain4j 0.35.0에서 ChatMemoryStore 인터페이스 확인 필요
 * 현재는 기본 구조만 구현, 실제 ChatMemoryStore 인터페이스 구현은 나중에 추가
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoDbChatMemoryStore {
    
    private final ConversationMessageRepository conversationMessageRepository;
    
    /**
     * 메시지 조회 (ChatMemoryStore.getMessages() 대체)
     */
    public List<ChatMessage> getMessages(String sessionId) {
        List<ConversationMessageDocument> documents = conversationMessageRepository
            .findBySessionIdOrderBySequenceNumberAsc(sessionId);
        
        return documents.stream()
            .map(this::toChatMessage)
            .collect(Collectors.toList());
    }
    
    /**
     * Document를 ChatMessage로 변환
     */
    private ChatMessage toChatMessage(ConversationMessageDocument doc) {
        String role = doc.getRole();
        String content = doc.getContent();
        
        return switch (role) {
            case "SYSTEM" -> new SystemMessage(content);
            case "USER" -> new UserMessage(content);
            case "ASSISTANT" -> new AiMessage(content);
            default -> throw new IllegalArgumentException("Unknown message role: " + role);
        };
    }
}
