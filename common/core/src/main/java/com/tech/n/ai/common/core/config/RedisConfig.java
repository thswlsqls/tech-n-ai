package com.tech.n.ai.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
}
