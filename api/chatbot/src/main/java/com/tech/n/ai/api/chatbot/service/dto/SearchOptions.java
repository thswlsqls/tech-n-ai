package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * 벡터 검색 옵션
 *
 * MongoDB Atlas Vector Search의 $vectorSearch 파라미터와 매핑됩니다.
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
 */
@Builder
public record SearchOptions(
    Boolean includeBookmarks,      // Bookmark 포함 여부
    Boolean includeEmergingTechs, // Emerging Tech 포함 여부
    Integer maxResults,           // 최대 결과 수 (limit)
    Integer numCandidates,        // 검색 후보 수 (limit의 10~20배 권장)
    Double minSimilarityScore,    // 최소 유사도 점수
    Boolean exact                 // ENN 검색 여부 (기본값: false, ANN 사용)
) {
    /**
     * 기본 검색 옵션
     *
     * @return 기본값이 설정된 SearchOptions
     */
    public static SearchOptions defaults() {
        return SearchOptions.builder()
            .includeBookmarks(true)
            .includeEmergingTechs(true)
            .maxResults(5)
            .numCandidates(100)
            .minSimilarityScore(0.7)
            .exact(false)
            .build();
    }

    /**
     * Bookmark만 검색하는 옵션 (userId 필터 필요)
     *
     * @return Bookmark 검색용 SearchOptions
     */
    public static SearchOptions bookmarksOnly() {
        return SearchOptions.builder()
            .includeBookmarks(true)
            .includeEmergingTechs(false)
            .maxResults(5)
            .numCandidates(100)
            .minSimilarityScore(0.7)
            .exact(false)
            .build();
    }
}
