package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 사용자 복원 이벤트
 */
public record UserRestoredEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") UserRestoredPayload payload
) implements BaseEvent {
    
    public UserRestoredEvent(UserRestoredPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "USER_RESTORED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * UserRestoredEvent 페이로드
     */
    public record UserRestoredPayload(
        @JsonProperty("userTsid") String userTsid,
        @JsonProperty("userId") String userId,
        @JsonProperty("username") String username,
        @JsonProperty("email") String email,
        @JsonProperty("profileImageUrl") String profileImageUrl
    ) {
    }
}

