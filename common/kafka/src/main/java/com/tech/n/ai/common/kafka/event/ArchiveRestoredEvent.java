package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 아카이브 복원 이벤트
 */
public record ArchiveRestoredEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ArchiveRestoredPayload payload
) implements BaseEvent {
    
    public ArchiveRestoredEvent(ArchiveRestoredPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "ARCHIVE_RESTORED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * ArchiveRestoredEvent 페이로드
     */
    public record ArchiveRestoredPayload(
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

