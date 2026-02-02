package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.ReRankingService;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResultRefinementChain {

    private final ReRankingService reRankingService;

    @Value("${chatbot.rag.max-search-results:5}")
    private int maxSearchResults;

    /**
     * 검색 결과 정제 (Re-Ranking 적용)
     *
     * @param query 원본 쿼리
     * @param rawResults 원본 검색 결과
     * @return 정제된 검색 결과
     */
    public List<SearchResult> refine(String query, List<SearchResult> rawResults) {
        // 1. 중복 제거
        List<SearchResult> deduplicated = removeDuplicates(rawResults);

        // 2. Re-Ranking 적용 (활성화된 경우)
        if (reRankingService.isEnabled()) {
            log.info("Applying Re-Ranking for query: {}", query);
            return reRankingService.rerank(query, deduplicated, maxSearchResults);
        }

        // 3. Re-Ranking 비활성화시 기존 score 기준 정렬
        return deduplicated.stream()
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .limit(maxSearchResults)
            .collect(Collectors.toList());
    }

    /**
     * 중복 제거
     */
    private List<SearchResult> removeDuplicates(List<SearchResult> results) {
        Set<String> seenIds = new HashSet<>();
        return results.stream()
            .filter(r -> seenIds.add(r.documentId()))
            .collect(Collectors.toList());
    }
}
