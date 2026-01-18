package com.tech.n.ai.common.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * 뉴스 기사 동기화 이벤트
 */
public record NewsArticleSyncedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") NewsArticleSyncedPayload payload
) implements BaseEvent {
    
    public NewsArticleSyncedEvent(NewsArticleSyncedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "NEWS_ARTICLE_SYNCED",
            Instant.now(),
            payload
        );
    }
    
    /**
     * NewsArticleSyncedEvent 페이로드
     */
    public record NewsArticleSyncedPayload(
        @JsonProperty("articleId") String articleId,
        @JsonProperty("sourceId") String sourceId,
        @JsonProperty("title") String title,
        @JsonProperty("summary") String summary,
        @JsonProperty("publishedAt") Instant publishedAt,
        @JsonProperty("url") String url
    ) {
    }
}

