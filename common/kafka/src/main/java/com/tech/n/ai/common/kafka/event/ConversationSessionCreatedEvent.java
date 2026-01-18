package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * 대화 세션 생성 이벤트
 */
public record ConversationSessionCreatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ConversationSessionCreatedPayload payload
) implements BaseEvent {
    
    public ConversationSessionCreatedEvent(ConversationSessionCreatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "CONVERSATION_SESSION_CREATED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ConversationSessionCreatedEvent 페이로드
     */
    public record ConversationSessionCreatedPayload(
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("userId") String userId,
        @JsonProperty("title") String title,
        @JsonProperty("lastMessageAt") Instant lastMessageAt,
        @JsonProperty("isActive") Boolean isActive
    ) {
    }
}
