package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 아카이브 수정 이벤트
 */
public record ArchiveUpdatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ArchiveUpdatedPayload payload
) implements BaseEvent {
    
    public ArchiveUpdatedEvent(ArchiveUpdatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "ARCHIVE_UPDATED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ArchiveUpdatedEvent 페이로드
     */
    public record ArchiveUpdatedPayload(
        @JsonProperty("archiveTsid") String archiveTsid,
        @JsonProperty("userId") String userId,
        @JsonProperty("updatedFields") Map<String, Object> updatedFields
    ) {
    }
}

