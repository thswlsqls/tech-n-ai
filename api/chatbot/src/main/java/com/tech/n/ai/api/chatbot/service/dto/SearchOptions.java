package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * 벡터 검색 옵션
 */
@Builder
public record SearchOptions(
    Boolean includeContests,      // Contest 포함 여부
    Boolean includeNews,           // News 포함 여부
    Boolean includeArchives,       // Archive 포함 여부
    Integer maxResults,             // 최대 결과 수
    Double minSimilarityScore      // 최소 유사도 점수
) {
    public static SearchOptions defaults() {
        return SearchOptions.builder()
            .includeContests(true)
            .includeNews(true)
            .includeArchives(true)
            .maxResults(5)
            .minSimilarityScore(0.7)
            .build();
    }
}
