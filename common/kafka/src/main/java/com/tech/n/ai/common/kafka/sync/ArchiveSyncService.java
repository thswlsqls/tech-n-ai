package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.ArchiveCreatedEvent;
import com.tech.n.ai.common.kafka.event.ArchiveDeletedEvent;
import com.tech.n.ai.common.kafka.event.ArchiveRestoredEvent;
import com.tech.n.ai.common.kafka.event.ArchiveUpdatedEvent;

/**
 * Archive 엔티티 동기화 서비스 인터페이스
 * 
 * Aurora MySQL의 Archive 엔티티 변경을 MongoDB Atlas의 ArchiveDocument에 동기화합니다.
 */
public interface ArchiveSyncService {
    
    /**
     * Archive 생성 이벤트 동기화
     * 
     * @param event ArchiveCreatedEvent
     */
    void syncArchiveCreated(ArchiveCreatedEvent event);
    
    /**
     * Archive 수정 이벤트 동기화
     * 
     * @param event ArchiveUpdatedEvent
     */
    void syncArchiveUpdated(ArchiveUpdatedEvent event);
    
    /**
     * Archive 삭제 이벤트 동기화 (Soft Delete)
     * MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제 수행
     * 
     * @param event ArchiveDeletedEvent
     */
    void syncArchiveDeleted(ArchiveDeletedEvent event);
    
    /**
     * Archive 복원 이벤트 동기화
     * MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
     * 
     * @param event ArchiveRestoredEvent
     */
    void syncArchiveRestored(ArchiveRestoredEvent event);
}
