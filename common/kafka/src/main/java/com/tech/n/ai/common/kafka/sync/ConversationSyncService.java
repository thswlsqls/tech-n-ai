package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.ConversationMessageCreatedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionCreatedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionDeletedEvent;
import com.tech.n.ai.common.kafka.event.ConversationSessionUpdatedEvent;

/**
 * 대화 세션 및 메시지 동기화 서비스 인터페이스
 * 
 * Aurora MySQL의 ConversationSession/Message 엔티티 변경을 MongoDB Atlas의 Document에 동기화합니다.
 */
public interface ConversationSyncService {
    
    /**
     * 세션 생성 이벤트 동기화
     * 
     * @param event ConversationSessionCreatedEvent
     */
    void syncSessionCreated(ConversationSessionCreatedEvent event);
    
    /**
     * 세션 수정 이벤트 동기화
     * 
     * @param event ConversationSessionUpdatedEvent
     */
    void syncSessionUpdated(ConversationSessionUpdatedEvent event);
    
    /**
     * 세션 삭제 이벤트 동기화 (Soft Delete)
     * MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제 수행
     * 
     * @param event ConversationSessionDeletedEvent
     */
    void syncSessionDeleted(ConversationSessionDeletedEvent event);
    
    /**
     * 메시지 생성 이벤트 동기화
     * 
     * @param event ConversationMessageCreatedEvent
     */
    void syncMessageCreated(ConversationMessageCreatedEvent event);
}
