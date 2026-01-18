package com.tech.n.ai.api.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 캐시 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    
    private static final String CACHE_KEY_PREFIX = "chatbot:cache:";
    
    @Qualifier("redisTemplateForObjects")
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${chatbot.cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${chatbot.cache.ttl-hours:1}")
    private int ttlHours;
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        if (!cacheEnabled) {
            return null;
        }
        
        try {
            String fullKey = CACHE_KEY_PREFIX + key;
            Object value = redisTemplate.opsForValue().get(fullKey);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get cache: key={}", key, e);
            return null;
        }
    }
    
    @Override
    public void put(String key, Object value, Duration ttl) {
        if (!cacheEnabled) {
            return;
        }
        
        try {
            String fullKey = CACHE_KEY_PREFIX + key;
            // Duration 객체 직접 사용 (프로젝트 표준)
            redisTemplate.opsForValue().set(fullKey, value, ttl);
        } catch (Exception e) {
            log.warn("Failed to put cache: key={}", key, e);
        }
    }
    
    @Override
    public void put(String key, Object value) {
        // 기본 TTL 사용 (ttl-hours 설정값)
        put(key, value, Duration.ofHours(ttlHours));
    }
    
    @Override
    public void delete(String key) {
        if (!cacheEnabled) {
            return;
        }
        
        try {
            String fullKey = CACHE_KEY_PREFIX + key;
            redisTemplate.delete(fullKey);
        } catch (Exception e) {
            log.warn("Failed to delete cache: key={}", key, e);
        }
    }
}
