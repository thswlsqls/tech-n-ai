package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 아카이브 생성 이벤트
 */
public record ArchiveCreatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ArchiveCreatedPayload payload
) implements BaseEvent {
    
    public ArchiveCreatedEvent(ArchiveCreatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "ARCHIVE_CREATED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ArchiveCreatedEvent 페이로드
     */
    public record ArchiveCreatedPayload(
        @JsonProperty("archiveTsid") String archiveTsid,
        @JsonProperty("userId") String userId,
        @JsonProperty("itemType") String itemType,
        @JsonProperty("itemId") String itemId,
        @JsonProperty("itemTitle") String itemTitle,
        @JsonProperty("itemSummary") String itemSummary,
        @JsonProperty("tag") String tag,
        @JsonProperty("memo") String memo,
        @JsonProperty("archivedAt") Instant archivedAt
    ) {
    }
}

