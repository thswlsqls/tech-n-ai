package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 아카이브 삭제 이벤트 (Soft Delete)
 */
public record ArchiveDeletedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ArchiveDeletedPayload payload
) implements BaseEvent {
    
    public ArchiveDeletedEvent(ArchiveDeletedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "ARCHIVE_DELETED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ArchiveDeletedEvent 페이로드
     */
    public record ArchiveDeletedPayload(
        @JsonProperty("archiveTsid") String archiveTsid,
        @JsonProperty("userId") String userId,
        @JsonProperty("deletedAt") Instant deletedAt
    ) {
    }
}

