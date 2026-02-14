package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.common.exception.ConversationSessionNotFoundException;
import com.tech.n.ai.api.chatbot.common.exception.InvalidInputException;
import com.tech.n.ai.api.chatbot.dto.response.SessionResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.common.kafka.event.ConversationSessionCreatedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionDeletedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionUpdatedEvent;
import com.tech.n.ai.common.kafka.publisher.EventPublisher;
import com.tech.n.ai.domain.mariadb.entity.chatbot.ConversationSessionEntity;
import com.tech.n.ai.domain.mariadb.repository.reader.chatbot.ConversationSessionReaderRepository;
import com.tech.n.ai.domain.mariadb.repository.writer.chatbot.ConversationSessionWriterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 대화 세션 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationSessionServiceImpl implements ConversationSessionService {

    private static final String TOPIC_SESSION_CREATED = "tech-n-ai.conversation.session.created";
    private static final String TOPIC_SESSION_UPDATED = "tech-n-ai.conversation.session.updated";
    private static final String TOPIC_SESSION_DELETED = "tech-n-ai.conversation.session.deleted";

    private final ConversationSessionWriterRepository conversationSessionWriterRepository;
    private final ConversationSessionReaderRepository conversationSessionReaderRepository;
    private final EventPublisher eventPublisher;
    
    @Override
    @Transactional
    public String createSession(Long userId, String title) {
        ConversationSessionEntity session = new ConversationSessionEntity();
        session.setUserId(userId);
        session.setTitle(title);
        session.setLastMessageAt(LocalDateTime.now());
        session.setIsActive(true);
        session.setCreatedBy(userId);
        
        ConversationSessionEntity savedSession = conversationSessionWriterRepository.save(session);
        
        // Kafka 이벤트 발행
        ConversationSessionCreatedEvent.ConversationSessionCreatedPayload payload = 
            new ConversationSessionCreatedEvent.ConversationSessionCreatedPayload(
                savedSession.getId().toString(),
                userId.toString(),
                savedSession.getTitle(),
                savedSession.getLastMessageAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                savedSession.getIsActive()
            );
        
        ConversationSessionCreatedEvent event = new ConversationSessionCreatedEvent(payload);
        eventPublisher.publish(TOPIC_SESSION_CREATED, event, savedSession.getId().toString());
        
        log.info("Session created: sessionId={}, userId={}", savedSession.getId(), userId);
        
        return savedSession.getId().toString();
    }
    
    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(String sessionId, Long userId) {
        ConversationSessionEntity session = validateSessionAccess(sessionId, userId);
        return toResponse(session);
    }
    
    @Override
    @Transactional
    public void updateLastMessageAt(String sessionId) {
        Long sessionIdLong = parseSessionId(sessionId);
        conversationSessionReaderRepository.findById(sessionIdLong).ifPresent(session -> {
            LocalDateTime now = LocalDateTime.now();
            session.setLastMessageAt(now);
            session.setUpdatedAt(now);
            
            // 메시지 교환 시 세션 자동 재활성화
            boolean wasInactive = !Boolean.TRUE.equals(session.getIsActive());
            if (wasInactive) {
                session.setIsActive(true);
                log.info("Session reactivated: sessionId={}", sessionId);
            }
            
            ConversationSessionEntity updatedSession = conversationSessionWriterRepository.save(session);

            Map<String, Object> updatedFields = new HashMap<>();
            updatedFields.put("lastMessageAt", updatedSession.getLastMessageAt());
            if (wasInactive) {
                updatedFields.put("isActive", true);
            }

            publishSessionUpdatedEvent(sessionId, updatedSession.getUserId().toString(), updatedFields);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SessionResponse> listSessions(Long userId, Pageable pageable) {
        Page<ConversationSessionEntity> sessionPage = conversationSessionReaderRepository
            .findByUserIdAndIsDeletedFalse(userId, pageable);
        
        return sessionPage.map(this::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> getActiveSession(Long userId) {
        return conversationSessionReaderRepository.findByUserIdAndIsActiveTrueAndIsDeletedFalse(userId)
            .map(this::toResponse);
    }
    
    @Override
    @Transactional
    public int deactivateInactiveSessions(Duration inactiveThreshold) {
        LocalDateTime thresholdTime = LocalDateTime.now().minus(inactiveThreshold);
        List<ConversationSessionEntity> inactiveSessions = conversationSessionReaderRepository
            .findByIsActiveTrueAndIsDeletedFalseAndLastMessageAtBefore(thresholdTime);
        
        inactiveSessions.forEach(session -> {
            session.setIsActive(false);
            session.setUpdatedAt(LocalDateTime.now());
            conversationSessionWriterRepository.save(session);
        });
        
        log.info("Deactivated {} inactive sessions (threshold: {} minutes)", 
            inactiveSessions.size(), inactiveThreshold.toMinutes());
        
        return inactiveSessions.size();
    }
    
    @Override
    @Transactional
    public int expireInactiveSessions(int expirationDays) {
        LocalDateTime expirationTime = LocalDateTime.now().minusDays(expirationDays);
        List<ConversationSessionEntity> expiredSessions = conversationSessionReaderRepository
            .findByIsActiveFalseAndIsDeletedFalseAndLastMessageAtBefore(expirationTime);
        
        log.info("Expired {} inactive sessions (expiration: {} days)", 
            expiredSessions.size(), expirationDays);
        
        return expiredSessions.size();
    }
    
    @Override
    @Transactional
    public SessionResponse updateSessionTitle(String sessionId, Long userId, String title) {
        ConversationSessionEntity session = validateSessionAccess(sessionId, userId);

        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        ConversationSessionEntity updatedSession = conversationSessionWriterRepository.save(session);

        publishSessionUpdatedEvent(sessionId, userId.toString(), Map.of("title", title));

        log.info("Session title updated: sessionId={}, title={}", sessionId, title);

        return toResponse(updatedSession);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        ConversationSessionEntity session = validateSessionAccess(sessionId, userId);

        session.setIsDeleted(true);
        session.setUpdatedAt(LocalDateTime.now());
        conversationSessionWriterRepository.save(session);
        
        ConversationSessionDeletedEvent.ConversationSessionDeletedPayload payload = 
            new ConversationSessionDeletedEvent.ConversationSessionDeletedPayload(
                sessionId,
                userId.toString(),
                LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
            );
        
        ConversationSessionDeletedEvent event = new ConversationSessionDeletedEvent(payload);
        eventPublisher.publish(TOPIC_SESSION_DELETED, event, sessionId);
        
        log.info("Session deleted: sessionId={}, userId={}", sessionId, userId);
    }
    
    private ConversationSessionEntity validateSessionAccess(String sessionId, Long userId) {
        Long sessionIdLong = parseSessionId(sessionId);
        ConversationSessionEntity session = conversationSessionReaderRepository.findById(sessionIdLong)
            .orElseThrow(() -> new ConversationSessionNotFoundException(
                "세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            log.warn("Unauthorized session access: sessionId={}, requestedUserId={}, actualUserId={}",
                sessionId, userId, session.getUserId());
            throw new UnauthorizedException("세션에 대한 접근 권한이 없습니다.");
        }

        if (Boolean.TRUE.equals(session.getIsDeleted())) {
            throw new ConversationSessionNotFoundException("삭제된 세션입니다: " + sessionId);
        }

        return session;
    }

    private void publishSessionUpdatedEvent(String sessionId, String userId,
                                             Map<String, Object> updatedFields) {
        ConversationSessionUpdatedEvent.ConversationSessionUpdatedPayload payload =
            new ConversationSessionUpdatedEvent.ConversationSessionUpdatedPayload(
                sessionId, userId, updatedFields);
        ConversationSessionUpdatedEvent event = new ConversationSessionUpdatedEvent(payload);
        eventPublisher.publish(TOPIC_SESSION_UPDATED, event, sessionId);
    }

    private SessionResponse toResponse(ConversationSessionEntity entity) {
        return SessionResponse.builder()
            .sessionId(entity.getId().toString())
            .title(entity.getTitle())
            .createdAt(entity.getCreatedAt())
            .lastMessageAt(entity.getLastMessageAt())
            .isActive(entity.getIsActive())
            .build();
    }

    private Long parseSessionId(String sessionId) {
        try {
            return Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("유효하지 않은 세션 ID 형식입니다: " + sessionId);
        }
    }
}
