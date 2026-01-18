package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * 대화 세션 삭제 이벤트
 */
public record ConversationSessionDeletedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ConversationSessionDeletedPayload payload
) implements BaseEvent {
    
    public ConversationSessionDeletedEvent(ConversationSessionDeletedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "CONVERSATION_SESSION_DELETED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ConversationSessionDeletedEvent 페이로드
     */
    public record ConversationSessionDeletedPayload(
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("userId") String userId,
        @JsonProperty("deletedAt") Instant deletedAt
    ) {
    }
}
