package com.tech.n.ai.datasource.mongodb.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * MongoDB 인덱스 자동 생성 설정
 * 
 * 애플리케이션 시작 시 모든 컬렉션의 인덱스를 자동으로 생성합니다.
 * 
 * **동작 방식**:
 * - `ensureIndex()` 메서드는 idempotent합니다 (멱등성 보장)
 * - 인덱스가 이미 존재하면 아무 작업도 하지 않습니다
 * - 인덱스가 없으면 생성합니다
 * 
 * **MongoDB Atlas 웹화면에서 수동 인덱스 생성 시**:
 * - Atlas 웹화면에서 먼저 인덱스를 생성해도 문제없습니다
 * - 애플리케이션 시작 시 `ensureIndex()`는 이미 존재하는 인덱스를 감지하고 스킵합니다
 * - 단, 인덱스 정의가 다르면 (필드 순서, 정렬 방향 등) 충돌이 발생할 수 있습니다
 * 
 * **권장 사항**:
 * - 개발 환경: 애플리케이션 코드로 자동 생성 (권장)
 * - 프로덕션 환경: 코드로 자동 생성 또는 Atlas 웹화면에서 수동 생성 모두 가능
 * - 인덱스 정의 변경 시: 기존 인덱스 삭제 후 재생성 필요
 * 
 * 참고: Spring Data MongoDB 공식 문서
 * https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#mongo.index-creation
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
     * 이미 존재하는 인덱스는 스킵되므로 안전하게 실행할 수 있습니다.
     */
    @PostConstruct
    public void createIndexes() {
        createSourcesIndexes();
        createContestsIndexes();
        createNewsArticlesIndexes();
        createExceptionLogsIndexes();
        createConversationSessionsIndexes();
        createConversationMessagesIndexes();
        
        log.info("MongoDB 인덱스 생성 완료: 모든 컬렉션의 인덱스가 확인되었습니다.");
    }

    private void createSourcesIndexes() {
        // category + priority 복합 인덱스 (ESR: E, S)
        mongoTemplate.indexOps("sources").ensureIndex(
            new Index().on("category", Sort.Direction.ASC)
                      .on("priority", Sort.Direction.ASC)
        );

        // type + enabled 복합 인덱스 (ESR: E, E)
        mongoTemplate.indexOps("sources").ensureIndex(
            new Index().on("type", Sort.Direction.ASC)
                      .on("enabled", Sort.Direction.ASC)
        );

        // priority 단일 인덱스
        mongoTemplate.indexOps("sources").ensureIndex(
            new Index().on("priority", Sort.Direction.ASC)
        );

        // url + category 복합 UNIQUE 인덱스 (UPSERT 키, ESR: E, E)
        // 동일한 URL이라도 다른 카테고리에 등록 가능
        mongoTemplate.indexOps("sources").ensureIndex(
            new Index().on("url", Sort.Direction.ASC)
                      .on("category", Sort.Direction.ASC)
                      .unique()
        );
    }

    private void createContestsIndexes() {
        // sourceId + startDate 복합 인덱스 (ESR: E, S)
        mongoTemplate.indexOps("contests").ensureIndex(
            new Index().on("source_id", Sort.Direction.ASC)
                      .on("start_date", Sort.Direction.DESC)
        );

        // sourceId 단일 인덱스 (외래 키 인덱스)
        mongoTemplate.indexOps("contests").ensureIndex(
            new Index().on("source_id", Sort.Direction.ASC)
        );

        // endDate 단일 인덱스
        mongoTemplate.indexOps("contests").ensureIndex(
            new Index().on("end_date", Sort.Direction.ASC)
        );

        // status + startDate 복합 인덱스 (부분 인덱스는 MongoDB에서 직접 생성 필요)
        mongoTemplate.indexOps("contests").ensureIndex(
            new Index().on("status", Sort.Direction.ASC)
                      .on("start_date", Sort.Direction.DESC)
        );
    }

    private void createNewsArticlesIndexes() {
        // sourceId + publishedAt 복합 인덱스 (ESR: E, S)
        mongoTemplate.indexOps("news_articles").ensureIndex(
            new Index().on("source_id", Sort.Direction.ASC)
                      .on("published_at", Sort.Direction.DESC)
        );

        mongoTemplate.indexOps("news_articles").ensureIndex(
            new Index().on("source_id", Sort.Direction.ASC)
                .on("url", Sort.Direction.DESC)
        );

        // sourceId 단일 인덱스 (외래 키 인덱스)
        mongoTemplate.indexOps("news_articles").ensureIndex(
            new Index().on("source_id", Sort.Direction.ASC)
        );

        // publishedAt TTL 인덱스 (90일 후 자동 삭제)
        mongoTemplate.indexOps("news_articles").ensureIndex(
            new Index().on("published_at", Sort.Direction.ASC)
                      .expire(7776000) // 90일 = 90 * 24 * 60 * 60
        );
    }

    private void createExceptionLogsIndexes() {
        // source + occurredAt 복합 인덱스 (ESR: E, S)
        mongoTemplate.indexOps("exception_logs").ensureIndex(
            new Index().on("source", Sort.Direction.ASC)
                      .on("occurred_at", Sort.Direction.DESC)
        );

        // exceptionType + occurredAt 복합 인덱스 (ESR: E, S)
        mongoTemplate.indexOps("exception_logs").ensureIndex(
            new Index().on("exception_type", Sort.Direction.ASC)
                      .on("occurred_at", Sort.Direction.DESC)
        );

        // occurredAt TTL 인덱스 (90일 후 자동 삭제)
        mongoTemplate.indexOps("exception_logs").ensureIndex(
            new Index().on("occurred_at", Sort.Direction.ASC)
                      .expire(7776000) // 90일 = 90 * 24 * 60 * 60
        );
    }

    private void createConversationSessionsIndexes() {
        // userId + isActive + lastMessageAt 복합 인덱스 (ESR 규칙)
        // Equality: userId, isActive, Sort: lastMessageAt
        mongoTemplate.indexOps("conversation_sessions").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("is_active", Sort.Direction.ASC)
                      .on("last_message_at", Sort.Direction.DESC)
        );

        // lastMessageAt TTL 인덱스 (90일 후 자동 삭제, 비활성 세션)
        // 주의: partialFilterExpression은 MongoDB에서 직접 설정 필요
        mongoTemplate.indexOps("conversation_sessions").ensureIndex(
            new Index().on("last_message_at", Sort.Direction.ASC)
                      .expire(7776000) // 90일 = 90 * 24 * 60 * 60
        );
    }

    private void createConversationMessagesIndexes() {
        // sessionId + sequenceNumber 복합 인덱스 (ESR 규칙)
        // Equality: sessionId, Sort: sequenceNumber
        mongoTemplate.indexOps("conversation_messages").ensureIndex(
            new Index().on("session_id", Sort.Direction.ASC)
                      .on("sequence_number", Sort.Direction.ASC)
        );

        // createdAt TTL 인덱스 (1년 후 자동 삭제, 오래된 메시지)
        mongoTemplate.indexOps("conversation_messages").ensureIndex(
            new Index().on("created_at", Sort.Direction.ASC)
                      .expire(31536000) // 1년 = 365 * 24 * 60 * 60
        );
    }
}
