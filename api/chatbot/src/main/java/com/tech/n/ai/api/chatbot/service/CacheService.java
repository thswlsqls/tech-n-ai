package com.tech.n.ai.api.chatbot.service;

import java.time.Duration;

/**
 * 캐시 서비스 인터페이스
 */
public interface CacheService {
    
    /**
     * 캐시 조회
     * 
     * @param key 캐시 키
     * @return 캐시된 값 (없으면 null)
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * 캐시 저장 (기본 TTL 사용)
     * 
     * @param key 캐시 키
     * @param value 캐시할 값
     */
    void put(String key, Object value);
    
    /**
     * 캐시 저장 (커스텀 TTL)
     * 
     * @param key 캐시 키
     * @param value 캐시할 값
     * @param ttl TTL (Duration 객체 직접 사용)
     */
    void put(String key, Object value, Duration ttl);
    
    /**
     * 캐시 삭제
     * 
     * @param key 캐시 키
     */
    void delete(String key);
}
