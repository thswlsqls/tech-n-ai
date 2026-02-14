package com.tech.n.ai.domain.mongodb.util;

import lombok.Builder;
import lombok.Value;
import org.bson.Document;

/**
 * MongoDB Atlas Vector Search 옵션
 * 
 * $vectorSearch aggregation stage의 파라미터를 정의합니다.
 * 
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
 */
@Value
@Builder
public class VectorSearchOptions {
    
    /**
     * Vector Search Index 이름
     */
    String indexName;
    
    /**
     * 벡터 필드 경로
     * 기본값: "embedding_vector"
     */
    String path;
    
    /**
     * 검색 후보 수
     * 기본값: 100 (limit의 10~20배 권장)
     * ANN 검색에서만 사용됨 (exact=true일 경우 무시)
     */
    int numCandidates;
    
    /**
     * 검색 결과 수 제한
     * 기본값: 5
     */
    int limit;
    
    /**
     * 최소 유사도 점수
     * 기본값: 0.7
     * $vectorSearch 이후 $match stage에서 필터링
     */
    double minScore;
    
    /**
     * 필터 조건 (선택사항)
     * MQL 연산자 사용: $eq, $in, $gte, $lte, $and, $or 등
     * 필터 필드는 Vector Search Index에 type: "filter"로 정의되어 있어야 함
     */
    Document filter;
    
    /**
     * ENN(Exact Nearest Neighbor) 검색 여부
     * 기본값: false (ANN 사용)
     * - false: ANN(Approximate Nearest Neighbor) - 빠르지만 근사치
     * - true: ENN(Exact Nearest Neighbor) - 정확하지만 느림, 소규모 데이터에 적합
     */
    boolean exact;

    /**
     * 파이프라인 내 Score Fusion 활성화 여부
     * false이면 기존 파이프라인 동작 유지
     */
    boolean enableScoreFusion;

    /**
     * 벡터 유사도 가중치 (Score Fusion용)
     * 기본값: 0.85 (일반 쿼리), 0.5 (최신성 쿼리)
     */
    double vectorWeight;

    /**
     * 최신성 가중치 (Score Fusion용)
     * 기본값: 0.15 (일반 쿼리), 0.5 (최신성 쿼리)
     */
    double recencyWeight;

    /**
     * Exponential Decay 계수 (λ)
     * recencyScore = e^(-λ × daysSincePublished)
     * 기본값: 1.0/365.0 ≈ 0.00274 (1년 기준)
     */
    double recencyDecayLambda;

    /**
     * 기본값이 설정된 Builder
     */
    public static class VectorSearchOptionsBuilder {
        private String path = "embedding_vector";
        private int numCandidates = 100;
        private int limit = 5;
        private double minScore = 0.7;
        private boolean exact = false;
        private boolean enableScoreFusion = false;
        private double vectorWeight = 0.85;
        private double recencyWeight = 0.15;
        private double recencyDecayLambda = 1.0 / 365.0;
    }
    
    /**
     * 기본 옵션으로 생성
     * 
     * @param indexName Vector Search Index 이름
     * @return 기본값이 설정된 VectorSearchOptions
     */
    public static VectorSearchOptions defaults(String indexName) {
        return VectorSearchOptions.builder()
            .indexName(indexName)
            .build();
    }
}
