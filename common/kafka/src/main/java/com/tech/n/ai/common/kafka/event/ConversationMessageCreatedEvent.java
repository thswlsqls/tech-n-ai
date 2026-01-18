package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * 대화 메시지 생성 이벤트
 */
public record ConversationMessageCreatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ConversationMessageCreatedPayload payload
) implements BaseEvent {
    
    public ConversationMessageCreatedEvent(ConversationMessageCreatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "CONVERSATION_MESSAGE_CREATED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ConversationMessageCreatedEvent 페이로드
     */
    public record ConversationMessageCreatedPayload(
        @JsonProperty("messageId") String messageId,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("role") String role,
        @JsonProperty("content") String content,
        @JsonProperty("tokenCount") Integer tokenCount,
        @JsonProperty("sequenceNumber") Integer sequenceNumber,
        @JsonProperty("createdAt") Instant createdAt
    ) {
    }
}
