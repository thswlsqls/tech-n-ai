package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.common.exception.ConversationSessionNotFoundException;
import com.tech.n.ai.api.chatbot.dto.response.SessionResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.common.kafka.event.ConversationSessionCreatedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionUpdatedEvent;
import com.tech.n.ai.common.kafka.publisher.EventPublisher;
import com.tech.n.ai.datasource.aurora.entity.chatbot.ConversationSessionEntity;
import com.tech.n.ai.datasource.aurora.repository.reader.chatbot.ConversationSessionReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.writer.chatbot.ConversationSessionWriterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
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
    
    private static final String KAFKA_TOPIC_CONVERSATION_EVENTS = "conversation-events";
    
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
                savedSession.getSessionId().toString(),
                userId.toString(),
                savedSession.getTitle(),
                savedSession.getLastMessageAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                savedSession.getIsActive()
            );
        
        ConversationSessionCreatedEvent event = new ConversationSessionCreatedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_CONVERSATION_EVENTS, event, savedSession.getSessionId().toString());
        
        log.debug("Session created: sessionId={}, userId={}", savedSession.getSessionId(), userId);
        
        return savedSession.getSessionId().toString();
    }
    
    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(String sessionId, Long userId) {
        Long sessionIdLong = Long.parseLong(sessionId);
        ConversationSessionEntity session = conversationSessionReaderRepository.findById(sessionIdLong)
            .orElseThrow(() -> new ConversationSessionNotFoundException("세션을 찾을 수 없습니다: " + sessionId));
        
        // 세션 소유권 검증 (보안)
        if (!session.getUserId().equals(userId)) {
            log.warn("Unauthorized session access attempt: sessionId={}, requestedUserId={}, actualUserId={}", 
                sessionId, userId, session.getUserId());
            throw new UnauthorizedException("세션에 대한 접근 권한이 없습니다.");
        }
        
        // Soft Delete 확인
        if (Boolean.TRUE.equals(session.getIsDeleted())) {
            throw new ConversationSessionNotFoundException("삭제된 세션입니다: " + sessionId);
        }
        
        return toResponse(session);
    }
    
    @Override
    @Transactional
    public void updateLastMessageAt(String sessionId) {
        Long sessionIdLong = Long.parseLong(sessionId);
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
            
            // Kafka 이벤트 발행 (업데이트)
            Map<String, Object> updatedFields = new HashMap<>();
            updatedFields.put("lastMessageAt", updatedSession.getLastMessageAt());
            if (wasInactive) {
                updatedFields.put("isActive", true);
            }
            
            ConversationSessionUpdatedEvent.ConversationSessionUpdatedPayload updatePayload = 
                new ConversationSessionUpdatedEvent.ConversationSessionUpdatedPayload(
                    sessionId,
                    updatedSession.getUserId().toString(),
                    updatedFields
                );
            
            ConversationSessionUpdatedEvent updateEvent = new ConversationSessionUpdatedEvent(updatePayload);
            eventPublisher.publish(KAFKA_TOPIC_CONVERSATION_EVENTS, updateEvent, sessionId);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SessionResponse> listSessions(Long userId, Pageable pageable) {
        // ConversationSessionReaderRepository에 페이징 메서드 추가 필요
        // 임시로 전체 조회 후 페이징 처리
        List<ConversationSessionEntity> allSessions = conversationSessionReaderRepository
            .findByUserIdAndIsDeletedFalse(userId);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allSessions.size());
        List<ConversationSessionEntity> pagedSessions = allSessions.subList(start, end);
        
        List<SessionResponse> responses = pagedSessions.stream()
            .map(this::toResponse)
            .collect(java.util.stream.Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(responses, pageable, allSessions.size());
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
        
        // 만료된 세션은 isActive = false 유지 (이미 비활성 상태)
        // 추가 처리 필요 시 여기에 구현 (예: 알림 발송, 통계 수집 등)
        // 참고: MongoDB TTL 인덱스가 Query Side에서 자동 삭제 처리
        
        log.info("Expired {} inactive sessions (expiration: {} days)", 
            expiredSessions.size(), expirationDays);
        
        return expiredSessions.size();
    }
    
    private SessionResponse toResponse(ConversationSessionEntity entity) {
        return SessionResponse.builder()
            .sessionId(entity.getSessionId().toString())
            .title(entity.getTitle())
            .createdAt(entity.getCreatedAt())
            .lastMessageAt(entity.getLastMessageAt())
            .isActive(entity.getIsActive())
            .build();
    }
}
