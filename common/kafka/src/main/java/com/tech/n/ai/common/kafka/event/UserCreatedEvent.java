package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 사용자 생성 이벤트
 */
public record UserCreatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") UserCreatedPayload payload
) implements BaseEvent {
    
    public UserCreatedEvent(UserCreatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "USER_CREATED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * UserCreatedEvent 페이로드
     */
    public record UserCreatedPayload(
        @JsonProperty("userTsid") String userTsid,
        @JsonProperty("userId") String userId,
        @JsonProperty("username") String username,
        @JsonProperty("email") String email,
        @JsonProperty("profileImageUrl") String profileImageUrl
    ) {
    }
}

