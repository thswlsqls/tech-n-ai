package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.UserCreatedEvent;
import com.tech.n.ai.common.kafka.event.UserDeletedEvent;
import com.tech.n.ai.common.kafka.event.UserRestoredEvent;
import com.tech.n.ai.common.kafka.event.UserUpdatedEvent;

/**
 * User 엔티티 동기화 서비스 인터페이스
 * 
 * Aurora MySQL의 User 엔티티 변경을 MongoDB Atlas의 UserProfileDocument에 동기화합니다.
 */
public interface UserSyncService {
    
    /**
     * User 생성 이벤트 동기화
     * 
     * @param event UserCreatedEvent
     */
    void syncUserCreated(UserCreatedEvent event);
    
    /**
     * User 수정 이벤트 동기화
     * 
     * @param event UserUpdatedEvent
     */
    void syncUserUpdated(UserUpdatedEvent event);
    
    /**
     * User 삭제 이벤트 동기화 (Soft Delete)
     * MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제 수행
     * 
     * @param event UserDeletedEvent
     */
    void syncUserDeleted(UserDeletedEvent event);
    
    /**
     * User 복원 이벤트 동기화
     * MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
     * 
     * @param event UserRestoredEvent
     */
    void syncUserRestored(UserRestoredEvent event);
}
