package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 사용자 삭제 이벤트 (Soft Delete)
 */
public record UserDeletedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") UserDeletedPayload payload
) implements BaseEvent {
    
    public UserDeletedEvent(UserDeletedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "USER_DELETED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * UserDeletedEvent 페이로드
     */
    public record UserDeletedPayload(
        @JsonProperty("userTsid") String userTsid,
        @JsonProperty("userId") String userId,
        @JsonProperty("deletedAt") Instant deletedAt
    ) {
    }
}

