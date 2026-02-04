package com.tech.n.ai.api.agent.tool.dto;

import java.util.List;

/**
 * 데이터 수집 결과 통계 DTO
 * LangChain4j @Tool 반환 시 JSON 직렬화되어 LLM에 전달됨
 */
public record DataCollectionResultDto(
    String source,              // "GITHUB_RELEASES", "RSS_FEEDS", "WEB_SCRAPING"
    String provider,            // 필터된 제공자 (빈 문자열이면 "ALL")
    int totalCollected,         // 외부 소스에서 수집된 원시 데이터 건수
    int totalProcessed,         // 유효성 검증 통과 후 Internal API에 전송한 건수
    int newCount,               // DB에 신규 저장된 건수
    int duplicateCount,         // 중복으로 스킵된 건수
    int failureCount,           // 저장 실패 건수
    List<String> failureMessages, // 실패 사유 목록
    String summary              // 사람이 읽을 수 있는 한 줄 요약
) {

    /**
     * 성공 결과 생성 팩토리 메서드
     */
    public static DataCollectionResultDto success(
            String source,
            String provider,
            int totalCollected,
            int totalProcessed,
            int newCount,
            int duplicateCount,
            int failureCount,
            List<String> failureMessages
    ) {
        String resolvedProvider = (provider == null || provider.isBlank()) ? "ALL" : provider;
        String summary = String.format(
            "%s(%s): 수집 %d건, 전송 %d건, 신규 %d건, 중복 %d건, 실패 %d건",
            source, resolvedProvider, totalCollected, totalProcessed,
            newCount, duplicateCount, failureCount
        );
        return new DataCollectionResultDto(
            source, resolvedProvider, totalCollected, totalProcessed,
            newCount, duplicateCount, failureCount, failureMessages, summary
        );
    }

    /**
     * 실패 결과 생성 팩토리 메서드
     */
    public static DataCollectionResultDto failure(
            String source,
            String provider,
            int totalCollected,
            String reason
    ) {
        String resolvedProvider = (provider == null || provider.isBlank()) ? "ALL" : provider;
        String summary = String.format(
            "%s(%s): 수집 %d건, 저장 실패 - %s",
            source, resolvedProvider, totalCollected, reason
        );
        return new DataCollectionResultDto(
            source, resolvedProvider, totalCollected, 0,
            0, 0, 0, List.of(reason), summary
        );
    }
}
