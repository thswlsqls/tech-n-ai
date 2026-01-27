package com.tech.n.ai.common.kafka.consumer;

import com.tech.n.ai.common.kafka.event.ConversationSessionCreatedEvent;
import com.tech.n.ai.common.kafka.sync.ConversationSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConversationSessionCreatedEventHandler implements EventHandler<ConversationSessionCreatedEvent> {
    
    @Autowired(required = false)
    private ConversationSyncService conversationSyncService;
    
    @Override
    public void handle(ConversationSessionCreatedEvent event) {
        if (conversationSyncService != null) {
            conversationSyncService.syncSessionCreated(event);
        } else {
            log.debug("ConversationSyncService not available, skipping sync: eventId={}", event.eventId());
        }
    }
    
    @Override
    public String getEventType() {
        return "CONVERSATION_SESSION_CREATED";
    }
}
