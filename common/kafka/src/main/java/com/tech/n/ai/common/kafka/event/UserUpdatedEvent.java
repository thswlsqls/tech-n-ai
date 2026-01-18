package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 사용자 업데이트 이벤트
 */
public record UserUpdatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") UserUpdatedPayload payload
) implements BaseEvent {
    
    public UserUpdatedEvent(UserUpdatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "USER_UPDATED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * UserUpdatedEvent 페이로드
     */
    public record UserUpdatedPayload(
        @JsonProperty("userTsid") String userTsid,
        @JsonProperty("userId") String userId,
        @JsonProperty("updatedFields") Map<String, Object> updatedFields
    ) {
    }
}

