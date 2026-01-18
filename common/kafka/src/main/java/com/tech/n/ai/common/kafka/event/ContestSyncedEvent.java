package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 대회 동기화 이벤트
 */
public record ContestSyncedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ContestSyncedPayload payload
) implements BaseEvent {
    
    public ContestSyncedEvent(ContestSyncedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "CONTEST_SYNCED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ContestSyncedEvent 페이로드
     */
    public record ContestSyncedPayload(
        @JsonProperty("contestId") String contestId,
        @JsonProperty("sourceId") String sourceId,
        @JsonProperty("title") String title,
        @JsonProperty("startDate") Instant startDate,
        @JsonProperty("endDate") Instant endDate,
        @JsonProperty("status") String status
    ) {
    }
}

