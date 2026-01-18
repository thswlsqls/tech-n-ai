package com.tech.n.ai.client.slack.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Slack Rate Limiter 유틸리티 클래스
 * Redis 기반 Rate Limiting을 통한 Slack API 호출 빈도 제어
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackRateLimiter {
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:slack:";
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * Rate Limiting 확인 및 대기
     * 최소 간격이 지나지 않았다면 대기
     * 
     * @param identifier 식별자 (예: "webhook", "bot:channel-id")
     * @param minIntervalMs 최소 간격 (밀리초)
     */
    public void checkAndWait(String identifier, long minIntervalMs) {
        if (minIntervalMs <= 0) {
            log.debug("Rate limiting disabled for {}", identifier);
            return;
        }
        
        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        String lastRequestTime = redisTemplate.opsForValue().get(key);
        
        if (lastRequestTime != null) {
            try {
                long lastTime = Long.parseLong(lastRequestTime);
                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - lastTime;
                
                if (elapsed < minIntervalMs) {
                    long waitTime = minIntervalMs - elapsed;
                    log.debug("Rate limiting: waiting {}ms for {}", waitTime, identifier);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Rate limiting interrupted", e);
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid last request time format for key {}: {}", key, lastRequestTime);
            }
        }
        
        // 요청 시간 저장 (TTL: 1분)
        redisTemplate.opsForValue().set(
            key, 
            String.valueOf(System.currentTimeMillis()), 
            Duration.ofMinutes(1)
        );
    }
}
