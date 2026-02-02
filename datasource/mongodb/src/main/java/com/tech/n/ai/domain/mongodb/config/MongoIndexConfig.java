package com.tech.n.ai.domain.mongodb.config;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB 인덱스 자동 생성 설정
 *
 * 애플리케이션 시작 시 모든 컬렉션의 인덱스를 자동으로 생성합니다.
 *
 * **동작 방식**:
 * - MongoDB Driver의 `createIndex()` 메서드를 직접 사용합니다 (멱등성 보장)
 * - 인덱스가 이미 존재하면 아무 작업도 하지 않습니다
 * - 인덱스가 없으면 생성합니다
 *
 * **MongoDB Atlas 웹화면에서 수동 인덱스 생성 시**:
 * - Atlas 웹화면에서 먼저 인덱스를 생성해도 문제없습니다
 * - 애플리케이션 시작 시 `createIndex()`는 이미 존재하는 인덱스를 감지하고 스킵합니다
 * - 단, 인덱스 정의가 다르면 (필드 순서, 정렬 방향 등) 충돌이 발생할 수 있습니다
 *
 * **권장 사항**:
 * - 개발 환경: 애플리케이션 코드로 자동 생성 (권장)
 * - 프로덕션 환경: 코드로 자동 생성 또는 Atlas 웹화면에서 수동 생성 모두 가능
 * - 인덱스 정의 변경 시: 기존 인덱스 삭제 후 재생성 필요
 *
 * 참고: MongoDB Java Driver 공식 문서
 * https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/indexes/
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    /**
     * 애플리케이션 시작 시 모든 컬렉션의 인덱스를 생성합니다.
     *
     * 주의: 이 메서드는 @PostConstruct로 애플리케이션 시작 시 자동 실행됩니다.
     * MongoDB Driver의 createIndex()는 멱등성을 보장하므로 안전하게 실행할 수 있습니다.
     */
    @PostConstruct
    public void createIndexes() {
        createExceptionLogsIndexes();
        createConversationSessionsIndexes();
        createConversationMessagesIndexes();
        createEmergingTechsIndexes();

        log.info("MongoDB 인덱스 생성 완료: 모든 컬렉션의 인덱스가 확인되었습니다.");
    }

    private void createExceptionLogsIndexes() {
        var collection = mongoTemplate.getCollection("exception_logs");

        // source + occurredAt 복합 인덱스 (ESR: E, S)
        collection.createIndex(Indexes.compoundIndex(
            Indexes.ascending("source"),
            Indexes.descending("occurred_at")
        ));

        // exceptionType + occurredAt 복합 인덱스 (ESR: E, S)
        collection.createIndex(Indexes.compoundIndex(
            Indexes.ascending("exception_type"),
            Indexes.descending("occurred_at")
        ));

        // occurredAt TTL 인덱스 (90일 후 자동 삭제)
        collection.createIndex(
            Indexes.ascending("occurred_at"),
            new IndexOptions().expireAfter(90L, TimeUnit.DAYS)
        );
    }

    private void createConversationSessionsIndexes() {
        var collection = mongoTemplate.getCollection("conversation_sessions");

        // userId + isActive + lastMessageAt 복합 인덱스 (ESR 규칙)
        // Equality: userId, isActive, Sort: lastMessageAt
        collection.createIndex(Indexes.compoundIndex(
            Indexes.ascending("user_id"),
            Indexes.ascending("is_active"),
            Indexes.descending("last_message_at")
        ));

        // lastMessageAt TTL 인덱스 (90일 후 자동 삭제, 비활성 세션)
        // 주의: partialFilterExpression은 MongoDB에서 직접 설정 필요
        collection.createIndex(
            Indexes.ascending("last_message_at"),
            new IndexOptions().expireAfter(90L, TimeUnit.DAYS)
        );
    }

    private void createEmergingTechsIndexes() {
        var collection = mongoTemplate.getCollection("emerging_techs");

        // url UNIQUE 인덱스 (중복 방지: 같은 URL은 소스가 달라도 중복)
        collection.createIndex(
            Indexes.ascending("url"),
            new IndexOptions().unique(true)
        );

        // provider + published_at 복합 인덱스 (ESR: E, S)
        collection.createIndex(Indexes.compoundIndex(
            Indexes.ascending("provider"),
            Indexes.descending("published_at")
        ));

        // status + published_at 복합 인덱스 (ESR: E, S)
        collection.createIndex(Indexes.compoundIndex(
            Indexes.ascending("status"),
            Indexes.descending("published_at")
        ));
    }

    private void createConversationMessagesIndexes() {
        var collection = mongoTemplate.getCollection("conversation_messages");

        // sessionId + sequenceNumber 복합 인덱스 (ESR 규칙)
        // Equality: sessionId, Sort: sequenceNumber
        collection.createIndex(Indexes.compoundIndex(
            Indexes.ascending("session_id"),
            Indexes.ascending("sequence_number")
        ));

        // createdAt TTL 인덱스 (1년 후 자동 삭제, 오래된 메시지)
        collection.createIndex(
            Indexes.ascending("created_at"),
            new IndexOptions().expireAfter(365L, TimeUnit.DAYS)
        );
    }
}
