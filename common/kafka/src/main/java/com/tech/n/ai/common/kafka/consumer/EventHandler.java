package com.tech.n.ai.common.kafka.consumer;

import com.tech.n.ai.common.kafka.event.BaseEvent;

public interface EventHandler<T extends BaseEvent> {
    void handle(T event);
    String getEventType();
}
