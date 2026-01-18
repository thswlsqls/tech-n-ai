package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.ConversationMessageCreatedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionCreatedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionDeletedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionUpdatedEvent;
import com.tech.n.ai.datasource.mongodb.document.ConversationMessageDocument;
import com.tech.n.ai.datasource.mongodb.document.ConversationSessionDocument;
import com.tech.n.ai.datasource.mongodb.repository.ConversationMessageRepository;
import com.tech.n.ai.datasource.mongodb.repository.ConversationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 대화 세션 및 메시지 동기화 서비스 구현 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationSyncServiceImpl implements ConversationSyncService {
    
    private final ConversationSessionRepository conversationSessionRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    
    @Override
    public void syncSessionCreated(ConversationSessionCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: sessionId로 조회하여 없으면 생성, 있으면 업데이트
            ConversationSessionDocument document = conversationSessionRepository
                .findBySessionId(payload.sessionId())
                .orElse(new ConversationSessionDocument());
            
            document.setSessionId(payload.sessionId());
            document.setUserId(payload.userId());
            document.setTitle(payload.title());
            document.setLastMessageAt(convertToLocalDateTime(payload.lastMessageAt()));
            document.setIsActive(payload.isActive());
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            conversationSessionRepository.save(document);
            
            log.debug("Successfully synced ConversationSessionCreatedEvent: sessionId={}, userId={}", 
                payload.sessionId(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ConversationSessionCreatedEvent: eventId={}, sessionId={}", 
                event.eventId(), event.payload().sessionId(), e);
            throw new RuntimeException("Failed to sync ConversationSessionCreatedEvent", e);
        }
    }
    
    @Override
    public void syncSessionUpdated(ConversationSessionUpdatedEvent event) {
        try {
            var payload = event.payload();
            var updatedFields = payload.updatedFields();
            
            // sessionId로 Document 조회
            ConversationSessionDocument document = conversationSessionRepository
                .findBySessionId(payload.sessionId())
                .orElseThrow(() -> new RuntimeException(
                    "ConversationSessionDocument not found: sessionId=" + payload.sessionId()));
            
            // updatedFields를 Document 필드에 매핑 (부분 업데이트)
            updateSessionDocumentFields(document, updatedFields);
            document.setUpdatedAt(LocalDateTime.now());
            
            conversationSessionRepository.save(document);
            
            log.debug("Successfully synced ConversationSessionUpdatedEvent: sessionId={}, updatedFields={}", 
                payload.sessionId(), updatedFields.keySet());
        } catch (Exception e) {
            log.error("Failed to sync ConversationSessionUpdatedEvent: eventId={}, sessionId={}", 
                event.eventId(), event.payload().sessionId(), e);
            throw new RuntimeException("Failed to sync ConversationSessionUpdatedEvent", e);
        }
    }
    
    @Override
    public void syncSessionDeleted(ConversationSessionDeletedEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제
            conversationSessionRepository.findBySessionId(payload.sessionId())
                .ifPresent(conversationSessionRepository::delete);
            
            log.debug("Successfully synced ConversationSessionDeletedEvent: sessionId={}, userId={}", 
                payload.sessionId(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ConversationSessionDeletedEvent: eventId={}, sessionId={}", 
                event.eventId(), event.payload().sessionId(), e);
            throw new RuntimeException("Failed to sync ConversationSessionDeletedEvent", e);
        }
    }
    
    @Override
    public void syncMessageCreated(ConversationMessageCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: messageId로 조회하여 없으면 생성, 있으면 업데이트
            ConversationMessageDocument document = conversationMessageRepository
                .findByMessageId(payload.messageId())
                .orElse(new ConversationMessageDocument());
            
            document.setMessageId(payload.messageId());
            document.setSessionId(payload.sessionId());
            document.setRole(payload.role());
            document.setContent(payload.content());
            document.setTokenCount(payload.tokenCount());
            document.setSequenceNumber(payload.sequenceNumber());
            document.setCreatedAt(convertToLocalDateTime(payload.createdAt()));
            
            conversationMessageRepository.save(document);
            
            log.debug("Successfully synced ConversationMessageCreatedEvent: messageId={}, sessionId={}", 
                payload.messageId(), payload.sessionId());
        } catch (Exception e) {
            log.error("Failed to sync ConversationMessageCreatedEvent: eventId={}, messageId={}", 
                event.eventId(), event.payload().messageId(), e);
            throw new RuntimeException("Failed to sync ConversationMessageCreatedEvent", e);
        }
    }
    
    /**
     * updatedFields를 Document 필드에 매핑 (부분 업데이트)
     */
    private void updateSessionDocumentFields(ConversationSessionDocument document, Map<String, Object> updatedFields) {
        for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            try {
                switch (fieldName) {
                    case "title":
                        document.setTitle((String) value);
                        break;
                    case "lastMessageAt":
                        if (value instanceof Instant instant) {
                            document.setLastMessageAt(convertToLocalDateTime(instant));
                        }
                        break;
                    case "isActive":
                        document.setIsActive((Boolean) value);
                        break;
                    default:
                        log.warn("Unknown field in updatedFields: {}", fieldName);
                }
            } catch (ClassCastException e) {
                log.warn("Type mismatch for field {}: {}", fieldName, value.getClass().getName());
            }
        }
    }
    
    /**
     * Instant를 LocalDateTime으로 변환
     */
    private LocalDateTime convertToLocalDateTime(Instant instant) {
        return instant != null 
            ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            : null;
    }
}
