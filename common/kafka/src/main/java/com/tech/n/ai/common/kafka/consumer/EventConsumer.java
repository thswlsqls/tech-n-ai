package com.tech.n.ai.common.kafka.consumer;

import com.tech.n.ai.common.kafka.event.*;
import com.tech.n.ai.common.kafka.sync.ArchiveSyncService;
import com.tech.n.ai.common.kafka.sync.ConversationSyncService;
import com.tech.n.ai.common.kafka.sync.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 이벤트 수신 및 처리 서비스
 * 
 * 참고:
 * - Spring Kafka 공식 문서: https://docs.spring.io/spring-kafka/reference/html/
 * - Apache Kafka Consumer API 공식 문서: https://kafka.apache.org/documentation/#consumerapi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumer {
    
    private static final String PROCESSED_EVENT_PREFIX = "processed_event:";
    private static final Duration PROCESSED_EVENT_TTL = Duration.ofDays(7);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final UserSyncService userSyncService;
    private final ArchiveSyncService archiveSyncService;
    private final ConversationSyncService conversationSyncService;
    
    /**
     * 이벤트 수신 및 처리
     * 
     * @param event 이벤트 객체
     * @param acknowledgment 수동 커밋용 Acknowledgment
     * @param partition 파티션 번호
     * @param offset 오프셋
     */
    @KafkaListener(
        topics = "${spring.kafka.consumer.topics:user-events,archive-events,contest-events,news-events,conversation-events}",
        groupId = "${spring.kafka.consumer.group-id:shrimp-task-manager-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
        @Payload BaseEvent event,
        Acknowledgment acknowledgment,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            // 멱등성 보장: 이벤트 ID 기반 중복 처리 방지
            if (isEventProcessed(event.eventId())) {
                log.warn("Event already processed, skipping: eventId={}, eventType={}, partition={}, offset={}", 
                    event.eventId(), event.eventType(), partition, offset);
                acknowledgment.acknowledge();
                return;
            }
            
            // 이벤트 처리 로직 (실제 구현은 각 도메인별로 처리)
            processEvent(event);
            
            // 처리 완료 표시 (Redis에 저장)
            markEventAsProcessed(event.eventId());
            
            // 수동 커밋
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed event: eventId={}, eventType={}, partition={}, offset={}", 
                event.eventId(), event.eventType(), partition, offset);
        } catch (Exception e) {
            log.error("Error processing event: eventId={}, eventType={}, partition={}, offset={}", 
                event.eventId(), event.eventType(), partition, offset, e);
            // 재시도는 Spring Kafka의 기본 재시도 메커니즘 사용
            throw e;
        }
    }
    
    /**
     * 이벤트 처리 (실제 구현은 각 도메인별로 처리)
     * 
     * @param event 이벤트 객체
     */
    private void processEvent(BaseEvent event) {
        String eventType = event.eventType();
        
        try {
            switch (eventType) {
                case "USER_CREATED":
                    if (event instanceof UserCreatedEvent userEvent) {
                        userSyncService.syncUserCreated(userEvent);
                    }
                    break;
                case "USER_UPDATED":
                    if (event instanceof UserUpdatedEvent userEvent) {
                        userSyncService.syncUserUpdated(userEvent);
                    }
                    break;
                case "USER_DELETED":
                    if (event instanceof UserDeletedEvent userEvent) {
                        userSyncService.syncUserDeleted(userEvent);
                    }
                    break;
                case "USER_RESTORED":
                    if (event instanceof UserRestoredEvent userEvent) {
                        userSyncService.syncUserRestored(userEvent);
                    }
                    break;
                case "ARCHIVE_CREATED":
                    if (event instanceof ArchiveCreatedEvent archiveEvent) {
                        archiveSyncService.syncArchiveCreated(archiveEvent);
                    }
                    break;
                case "ARCHIVE_UPDATED":
                    if (event instanceof ArchiveUpdatedEvent archiveEvent) {
                        archiveSyncService.syncArchiveUpdated(archiveEvent);
                    }
                    break;
                case "ARCHIVE_DELETED":
                    if (event instanceof ArchiveDeletedEvent archiveEvent) {
                        archiveSyncService.syncArchiveDeleted(archiveEvent);
                    }
                    break;
                case "ARCHIVE_RESTORED":
                    if (event instanceof ArchiveRestoredEvent archiveEvent) {
                        archiveSyncService.syncArchiveRestored(archiveEvent);
                    }
                    break;
                case "CONVERSATION_SESSION_CREATED":
                    if (event instanceof ConversationSessionCreatedEvent sessionEvent) {
                        conversationSyncService.syncSessionCreated(sessionEvent);
                    }
                    break;
                case "CONVERSATION_SESSION_UPDATED":
                    if (event instanceof ConversationSessionUpdatedEvent sessionEvent) {
                        conversationSyncService.syncSessionUpdated(sessionEvent);
                    }
                    break;
                case "CONVERSATION_SESSION_DELETED":
                    if (event instanceof ConversationSessionDeletedEvent sessionEvent) {
                        conversationSyncService.syncSessionDeleted(sessionEvent);
                    }
                    break;
                case "CONVERSATION_MESSAGE_CREATED":
                    if (event instanceof ConversationMessageCreatedEvent messageEvent) {
                        conversationSyncService.syncMessageCreated(messageEvent);
                    }
                    break;
                case "CONTEST_SYNCED":
                case "NEWS_ARTICLE_SYNCED":
                    // 배치 작업에서 직접 MongoDB에 저장되므로 동기화 불필요
                    // 로깅만 수행
                    log.debug("Skipping sync for batch event: eventType={}, eventId={}", 
                        eventType, event.eventId());
                    break;
                default:
                    log.warn("Unknown event type: eventType={}, eventId={}", 
                        eventType, event.eventId());
            }
        } catch (Exception e) {
            log.error("Error processing event: eventType={}, eventId={}", 
                eventType, event.eventId(), e);
            throw e; // 예외 전파하여 Spring Kafka 재시도 메커니즘 활용
        }
    }
    
    /**
     * 이벤트가 이미 처리되었는지 확인 (멱등성 보장)
     * 
     * @param eventId 이벤트 ID
     * @return 이미 처리되었으면 true
     */
    private boolean isEventProcessed(String eventId) {
        String key = PROCESSED_EVENT_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * 이벤트를 처리 완료로 표시 (Redis에 저장)
     * 
     * @param eventId 이벤트 ID
     */
    private void markEventAsProcessed(String eventId) {
        String key = PROCESSED_EVENT_PREFIX + eventId;
        // Duration 객체 직접 사용 (일관성 개선)
        redisTemplate.opsForValue().set(key, "processed", PROCESSED_EVENT_TTL);
    }
}

