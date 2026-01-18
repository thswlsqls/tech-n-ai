package com.tech.n.ai.common.kafka.event;

import java.time.Instant;

/**
 * 이벤트 베이스 인터페이스
 * 모든 이벤트가 공통 필드를 가지도록 정의
 */
public interface BaseEvent {
    /**
     * 이벤트 ID (UUID 형식, 고유 식별자)
     */
    String eventId();
    
    /**
     * 이벤트 타입 (예: "USER_CREATED", "ARCHIVE_CREATED")
     */
    String eventType();
    
    /**
     * 이벤트 발생 시각
     */
    Instant timestamp();
}

