package com.tech.n.ai.datasource.mongodb.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * MongoIndexConfig
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void createIndexes() {
        createSourcesIndexes();
        createContestsIndexes();
        createNewsArticlesIndexes();
        createArchivesIndexes();
        createUserProfilesIndexes();
        createExceptionLogsIndexes();
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

    private void createArchivesIndexes() {
        // userId + createdAt 복합 인덱스 (ESR: E, S)
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("created_at", Sort.Direction.DESC)
        );

        // userId + itemType + createdAt 복합 인덱스 (ESR: E, E, S)
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("item_type", Sort.Direction.ASC)
                      .on("created_at", Sort.Direction.DESC)
        );

        // userId + itemType + itemId UNIQUE 복합 인덱스 (ESR: E, E, E)
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("item_type", Sort.Direction.ASC)
                      .on("item_id", Sort.Direction.ASC)
                      .unique()
        );

        // itemId 단일 인덱스
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("item_id", Sort.Direction.ASC)
        );

        // userId + itemStartDate 복합 인덱스 (ESR: E, S) - Contest 정렬 시
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("item_start_date", Sort.Direction.DESC)
        );

        // userId + itemEndDate 복합 인덱스 (ESR: E, S) - Contest 정렬 시
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("item_end_date", Sort.Direction.DESC)
        );

        // userId + itemPublishedAt 복합 인덱스 (ESR: E, S) - News 정렬 시
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("item_published_at", Sort.Direction.DESC)
        );

        // userId + archivedAt 복합 인덱스 (ESR: E, S)
        mongoTemplate.indexOps("archives").ensureIndex(
            new Index().on("user_id", Sort.Direction.ASC)
                      .on("archived_at", Sort.Direction.DESC)
        );
    }

    private void createUserProfilesIndexes() {
        // userTsid, userId, username, email은 이미 @Indexed(unique = true)로 설정됨
        // 추가 인덱스가 필요한 경우 여기에 추가
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
}
