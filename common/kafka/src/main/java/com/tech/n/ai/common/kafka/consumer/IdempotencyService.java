package com.tech.n.ai.common.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private static final String PROCESSED_EVENT_PREFIX = "processed_event:";
    private static final Duration PROCESSED_EVENT_TTL = Duration.ofDays(7);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isEventProcessed(String eventId) {
        String key = PROCESSED_EVENT_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public void markEventAsProcessed(String eventId) {
        String key = PROCESSED_EVENT_PREFIX + eventId;
        redisTemplate.opsForValue().set(key, "processed", PROCESSED_EVENT_TTL);
    }
}
