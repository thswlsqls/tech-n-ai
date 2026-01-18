package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 대화 세션 업데이트 이벤트
 */
public record ConversationSessionUpdatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ConversationSessionUpdatedPayload payload
) implements BaseEvent {
    
    public ConversationSessionUpdatedEvent(ConversationSessionUpdatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "CONVERSATION_SESSION_UPDATED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ConversationSessionUpdatedEvent 페이로드
     */
    public record ConversationSessionUpdatedPayload(
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("userId") String userId,
        @JsonProperty("updatedFields") Map<String, Object> updatedFields
    ) {
    }
}
