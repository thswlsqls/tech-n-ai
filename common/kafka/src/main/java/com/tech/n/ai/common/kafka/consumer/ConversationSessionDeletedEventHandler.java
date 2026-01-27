package com.tech.n.ai.common.kafka.consumer;

import com.tech.n.ai.common.kafka.event.ConversationSessionDeletedEvent;
import com.tech.n.ai.common.kafka.sync.ConversationSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConversationSessionDeletedEventHandler implements EventHandler<ConversationSessionDeletedEvent> {
    
    @Autowired(required = false)
    private ConversationSyncService conversationSyncService;
    
    @Override
    public void handle(ConversationSessionDeletedEvent event) {
        if (conversationSyncService != null) {
            conversationSyncService.syncSessionDeleted(event);
        } else {
            log.debug("ConversationSyncService not available, skipping sync: eventId={}", event.eventId());
        }
    }
    
    @Override
    public String getEventType() {
        return "CONVERSATION_SESSION_DELETED";
    }
}
