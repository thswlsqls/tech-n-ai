package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 벡터 검색 옵션
 *
 * MongoDB Atlas Vector Search의 $vectorSearch 파라미터와 매핑됩니다.
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
 */
@Builder
public record SearchOptions(
    Boolean includeEmergingTechs, // Emerging Tech 포함 여부
    Integer maxResults,           // 최대 결과 수 (limit)
    Integer numCandidates,        // 검색 후보 수 (limit의 10~20배 권장)
    Double minSimilarityScore,    // 최소 유사도 점수
    Boolean exact,                // ENN 검색 여부 (기본값: false, ANN 사용)
    List<String> providerFilters,  // provider pre-filter (TechProvider enum values, null/empty이면 필터 미적용)
    Boolean recencyDetected,      // 최신성 키워드 감지 여부
    LocalDateTime dateFrom,       // 날짜 pre-filter 기준 (이 날짜 이후 문서만 검색)
    Boolean enableScoreFusion,    // Score Fusion 활성화 여부 (기본값: null → false)
    List<String> updateTypeFilters, // update_type pre-filter (EmergingTechType enum values, null/empty이면 필터 미적용)
    List<String> sourceTypeFilters  // source_type pre-filter (SourceType enum values, null/empty이면 필터 미적용)
) {
    /**
     * 기본 검색 옵션 (Emerging Tech 전용)
     *
     * @return 기본값이 설정된 SearchOptions
     */
    public static SearchOptions defaults() {
        return SearchOptions.builder()
            .includeEmergingTechs(true)
            .maxResults(5)
            .numCandidates(100)
            .minSimilarityScore(0.7)
            .exact(false)
            .build();
    }
}
