package com.tech.n.ai.common.kafka.consumer;

import com.tech.n.ai.common.kafka.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EventHandlerRegistry {
    
    private final Map<String, EventHandler<? extends BaseEvent>> handlers = new HashMap<>();
    
    public EventHandlerRegistry(List<EventHandler<? extends BaseEvent>> handlerList) {
        handlerList.forEach(handler -> 
            handlers.put(handler.getEventType(), handler)
        );
    }
    
    @SuppressWarnings("unchecked")
    public <T extends BaseEvent> void handle(T event) {
        EventHandler<T> handler = (EventHandler<T>) handlers.get(event.eventType());
        
        if (handler == null) {
            log.warn("No handler found for event type: {}", event.eventType());
            return;
        }
        
        handler.handle(event);
    }
    
    public boolean hasHandler(String eventType) {
        return handlers.containsKey(eventType);
    }
}
