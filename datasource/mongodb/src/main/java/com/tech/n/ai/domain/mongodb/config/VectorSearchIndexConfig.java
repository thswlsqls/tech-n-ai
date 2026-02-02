package com.tech.n.ai.domain.mongodb.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * MongoDB Atlas Vector Search Index 설정 유틸리티
 *
 * 참고: Vector Search Index는 MongoDB Atlas에서만 생성 가능합니다.
 * Spring Data MongoDB로는 생성할 수 없으므로, 이 클래스는 Index 정의만 제공하며,
 * 실제 생성은 Atlas UI 또는 Atlas CLI에서 수행해야 합니다.
 *
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/
 */
@Slf4j
@Configuration
public class VectorSearchIndexConfig {

    // Vector Search Index 이름
    public static final String INDEX_NAME_EMERGING_TECHS = "vector_index_emerging_techs";

    // 벡터 차원 수 (OpenAI text-embedding-3-small)
    public static final int VECTOR_DIMENSIONS = 1536;

    // 유사도 함수
    public static final String SIMILARITY_COSINE = "cosine";
    public static final String SIMILARITY_EUCLIDEAN = "euclidean";
    public static final String SIMILARITY_DOT_PRODUCT = "dotProduct";

    /**
     * EmergingTechDocument Vector Search Index 정의
     *
     * @return Index 정의 JSON 문자열
     */
    public static String getEmergingTechVectorIndexDefinition() {
        return """
            {
              "fields": [
                {
                  "type": "vector",
                  "path": "embedding_vector",
                  "numDimensions": 1536,
                  "similarity": "cosine"
                },
                {
                  "type": "filter",
                  "path": "provider"
                },
                {
                  "type": "filter",
                  "path": "status"
                }
              ]
            }
            """;
    }

    /**
     * Atlas CLI를 사용한 Index 생성 명령어 반환
     *
     * @param clusterName 클러스터 이름
     * @param databaseName 데이터베이스 이름
     * @param collectionName 컬렉션 이름
     * @param indexName 인덱스 이름
     * @param indexDefinition 인덱스 정의 JSON
     * @return Atlas CLI 명령어
     */
    public static String getAtlasCliCommand(
            String clusterName,
            String databaseName,
            String collectionName,
            String indexName,
            String indexDefinition) {
        return String.format("""
            atlas clusters search indexes create %s \\
              --clusterName %s \\
              --db %s \\
              --collection %s \\
              --type vectorSearch \\
              --file <(echo '%s')
            """, indexName, clusterName, databaseName, collectionName,
            indexDefinition.replaceAll("\\s+", " ").trim());
    }

    /**
     * 애플리케이션 시작 시 Vector Search Index 생성 가이드 로깅
     */
    @PostConstruct
    public void logIndexCreationGuide() {
        log.info("==================================================");
        log.info("MongoDB Atlas Vector Search Index 생성 가이드");
        log.info("==================================================");
        log.info("Vector Search Index는 MongoDB Atlas에서만 생성 가능합니다.");
        log.info("Atlas UI 또는 Atlas CLI를 사용하여 Index를 생성하세요.");
        log.info("");
        log.info("필요한 Vector Search Index:");
        log.info("  1. {} (emerging_techs 컬렉션)", INDEX_NAME_EMERGING_TECHS);
        log.info("");
        log.info("가이드 문서: docs/step1/6. mongodb-atlas-integration-guide.md");
        log.info("공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/");
        log.info("==================================================");
    }

    /**
     * 모든 Vector Search Index 정의 로깅 (디버그용)
     */
    public void logAllIndexDefinitions() {
        log.debug("=== EmergingTech Vector Index Definition ===");
        log.debug(getEmergingTechVectorIndexDefinition());
    }
}
