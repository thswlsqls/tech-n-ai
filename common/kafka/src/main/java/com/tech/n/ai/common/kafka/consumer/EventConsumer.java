package com.tech.n.ai.common.kafka.consumer;

import com.tech.n.ai.common.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumer {
    
    private final IdempotencyService idempotencyService;
    private final EventHandlerRegistry eventHandlerRegistry;
    
    @KafkaListener(
        topics = "#{'${spring.kafka.consumer.topics:shrimp-tm.conversation.session.created,shrimp-tm.conversation.session.updated,shrimp-tm.conversation.session.deleted,shrimp-tm.conversation.message.created}'.split(',')}",
        groupId = "${spring.kafka.consumer.group-id:shrimp-tm-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
        @Payload BaseEvent event,
        Acknowledgment acknowledgment,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            if (shouldSkipEvent(event, partition, offset)) {
                acknowledgment.acknowledge();
                return;
            }
            
            processEvent(event);
            idempotencyService.markEventAsProcessed(event.eventId());
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed event: eventId={}, eventType={}, partition={}, offset={}", 
                event.eventId(), event.eventType(), partition, offset);
        } catch (Exception e) {
            log.error("Error processing event: eventId={}, eventType={}, partition={}, offset={}", 
                event.eventId(), event.eventType(), partition, offset, e);
            throw e;
        }
    }
    
    private boolean shouldSkipEvent(BaseEvent event, int partition, long offset) {
        if (idempotencyService.isEventProcessed(event.eventId())) {
            log.warn("Event already processed, skipping: eventId={}, eventType={}, partition={}, offset={}", 
                event.eventId(), event.eventType(), partition, offset);
            return true;
        }
        return false;
    }
    
    private void processEvent(BaseEvent event) {
        try {
            if (!eventHandlerRegistry.hasHandler(event.eventType())) {
                log.warn("No handler registered for event type: eventType={}, eventId={}", 
                    event.eventType(), event.eventId());
                return;
            }
            
            eventHandlerRegistry.handle(event);
        } catch (Exception e) {
            log.error("Error processing event: eventType={}, eventId={}", 
                event.eventType(), event.eventId(), e);
            throw e;
        }
    }
}
