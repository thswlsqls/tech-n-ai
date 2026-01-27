package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.common.exception.ConversationSessionNotFoundException;
import com.tech.n.ai.api.chatbot.dto.response.MessageResponse;
import com.tech.n.ai.api.chatbot.memory.MongoDbChatMemoryStore;
import com.tech.n.ai.common.kafka.event.ConversationMessageCreatedEvent;
import com.tech.n.ai.common.kafka.publisher.EventPublisher;
import com.tech.n.ai.datasource.mariadb.entity.chatbot.ConversationMessageEntity;
import com.tech.n.ai.datasource.mariadb.entity.chatbot.ConversationSessionEntity;
import com.tech.n.ai.datasource.mariadb.repository.reader.chatbot.ConversationSessionReaderRepository;
import com.tech.n.ai.datasource.mariadb.repository.writer.chatbot.ConversationMessageWriterRepository;
import com.tech.n.ai.datasource.mongodb.document.ConversationMessageDocument;
import com.tech.n.ai.datasource.mongodb.repository.ConversationMessageRepository;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 대화 메시지 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageServiceImpl implements ConversationMessageService {
    
    private static final String KAFKA_TOPIC_CONVERSATION_EVENTS = "conversation-events";
    
    private final ConversationMessageWriterRepository conversationMessageWriterRepository;
    private final com.tech.n.ai.datasource.mariadb.repository.writer.chatbot.ConversationMessageWriterJpaRepository conversationMessageWriterJpaRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationSessionReaderRepository conversationSessionReaderRepository;
    private final MongoDbChatMemoryStore mongoDbChatMemoryStore;
    private final EventPublisher eventPublisher;
    
    @Override
    @Transactional
    public void saveMessage(String sessionId, String role, String content, Integer tokenCount) {
        Long sessionIdLong = Long.parseLong(sessionId);
        
        // 세션 존재 확인
        ConversationSessionEntity session = conversationSessionReaderRepository.findById(sessionIdLong)
            .orElseThrow(() -> new ConversationSessionNotFoundException("세션을 찾을 수 없습니다: " + sessionId));
        
        // 다음 sequence number 계산
        List<ConversationMessageEntity> existingMessages = conversationMessageWriterJpaRepository
            .findBySessionIdOrderBySequenceNumberAsc(sessionIdLong);
        int nextSequenceNumber = existingMessages.isEmpty() ? 1 : 
            existingMessages.get(existingMessages.size() - 1).getSequenceNumber() + 1;
        
        // 메시지 엔티티 생성 및 저장
        ConversationMessageEntity message = new ConversationMessageEntity();
        message.setSession(session);
        message.setRole(ConversationMessageEntity.MessageRole.valueOf(role));
        message.setContent(content);
        message.setTokenCount(tokenCount);
        message.setSequenceNumber(nextSequenceNumber);
        message.setCreatedAt(LocalDateTime.now());
        
        ConversationMessageEntity savedMessage = conversationMessageWriterRepository.save(message);
        
        // Kafka 이벤트 발행
        ConversationMessageCreatedEvent.ConversationMessageCreatedPayload payload = 
            new ConversationMessageCreatedEvent.ConversationMessageCreatedPayload(
                savedMessage.getMessageId().toString(),
                sessionId,
                role,
                content,
                tokenCount,
                nextSequenceNumber,
                savedMessage.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
            );
        
        ConversationMessageCreatedEvent event = new ConversationMessageCreatedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_CONVERSATION_EVENTS, event, sessionId);
        
        log.debug("Message saved: sessionId={}, role={}, sequenceNumber={}", 
            sessionId, role, nextSequenceNumber);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(String sessionId, Pageable pageable) {
        try {
            Page<ConversationMessageDocument> mongoPage = conversationMessageRepository
                .findBySessionIdOrderBySequenceNumberAsc(sessionId, pageable);
            
            if (mongoPage.hasContent()) {
                return mongoPage.map(this::toResponse);
            }
        } catch (Exception e) {
            log.warn("Failed to get messages from MongoDB, falling back to Aurora MySQL: sessionId={}", 
                sessionId, e);
        }
        
        Long sessionIdLong = Long.parseLong(sessionId);
        List<ConversationMessageEntity> messages = conversationMessageWriterJpaRepository
            .findBySessionIdOrderBySequenceNumberAsc(sessionIdLong);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), messages.size());
        List<ConversationMessageEntity> pagedMessages = messages.subList(start, end);
        
        List<MessageResponse> responses = pagedMessages.stream()
            .map(this::toResponseFromEntity)
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, messages.size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForMemory(String sessionId, Integer maxTokens) {
        // ChatMemory용 메시지 조회 (MongoDbChatMemoryStore 사용)
        List<ChatMessage> messages = mongoDbChatMemoryStore.getMessages(sessionId);
        
        // TODO: maxTokens 제한 적용 (TokenService 사용)
        // 현재는 모든 메시지 반환
        
        return messages;
    }
    
    private MessageResponse toResponse(ConversationMessageDocument doc) {
        return MessageResponse.builder()
            .messageId(doc.getMessageId())
            .sessionId(doc.getSessionId())
            .role(doc.getRole())
            .content(doc.getContent())
            .tokenCount(doc.getTokenCount())
            .sequenceNumber(doc.getSequenceNumber())
            .createdAt(doc.getCreatedAt())
            .build();
    }
    
    private MessageResponse toResponseFromEntity(ConversationMessageEntity entity) {
        return MessageResponse.builder()
            .messageId(entity.getMessageId().toString())
            .sessionId(entity.getSession().getId().toString())
            .role(entity.getRole().name())
            .content(entity.getContent())
            .tokenCount(entity.getTokenCount())
            .sequenceNumber(entity.getSequenceNumber())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
