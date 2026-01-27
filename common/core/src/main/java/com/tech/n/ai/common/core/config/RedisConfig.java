package com.tech.n.ai.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 (챗봇 캐싱용 확장)
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        // 기본 직렬화 비활성화 (명시적 직렬화만 사용)
        template.setEnableDefaultSerializer(false);
        
        // 트랜잭션 지원 비활성화 (현재 사용 사례에서 불필요)
        template.setEnableTransactionSupport(false);
        
        // 초기화
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 복잡한 객체 저장용 RedisTemplate (챗봇 캐싱 등)
     * 
     * 프로젝트의 Redis 최적화 베스트 프랙티스에 따라 JSON 직렬화 사용
     * 참고: docs/step7/redis-optimization-best-practices.md
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplateForObjects(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key는 String 직렬화 (프로젝트 표준)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value는 JSON 직렬화 (복잡한 객체 저장용)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        // 기본 직렬화 비활성화 (명시적 직렬화만 사용)
        template.setEnableDefaultSerializer(false);
        
        // 트랜잭션 지원 비활성화 (캐싱 사용 사례에서 불필요)
        template.setEnableTransactionSupport(false);
        
        // 초기화
        template.afterPropertiesSet();
        
        return template;
    }
}
