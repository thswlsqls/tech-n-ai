package com.tech.n.ai.common.kafka.publisher;

import com.tech.n.ai.common.kafka.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 이벤트 발행 서비스
 * 
 * 참고:
 * - Spring Kafka 공식 문서: https://docs.spring.io/spring-kafka/reference/html/
 * - Apache Kafka Producer API 공식 문서: https://kafka.apache.org/documentation/#producerapi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * 이벤트 발행
     * 
     * @param topic Kafka 토픽
     * @param event 이벤트 객체
     * @param partitionKey Partition Key (이벤트 순서 보장용, userId, archiveId 등)
     */
    public void publish(String topic, BaseEvent event, String partitionKey) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                topic,
                partitionKey,
                event
            );
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish event to topic: {}, eventId: {}, eventType: {}", 
                        topic, event.eventId(), event.eventType(), exception);
                } else {
                    log.debug("Successfully published event to topic: {}, eventId: {}, eventType: {}, partition: {}, offset: {}", 
                        topic, event.eventId(), event.eventType(), 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing event to topic: {}, eventId: {}, eventType: {}", 
                topic, event.eventId(), event.eventType(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
    
    /**
     * 이벤트 발행 (기본 Partition Key 사용)
     * 
     * @param topic Kafka 토픽
     * @param event 이벤트 객체
     */
    public void publish(String topic, BaseEvent event) {
        // eventId를 기본 Partition Key로 사용
        publish(topic, event, event.eventId());
    }
}

