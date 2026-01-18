package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.dto.RefinedResult;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 검색 결과 정제 체인
 */
@Slf4j
@Component
public class ResultRefinementChain {
    
    @Value("${chatbot.rag.min-similarity-score:0.7}")
    private double minSimilarityScore;
    
    /**
     * 검색 결과 정제
     * 
     * @param rawResults 원본 검색 결과
     * @return 정제된 검색 결과
     */
    public List<RefinedResult> refine(List<SearchResult> rawResults) {
        // 1. 유사도 점수 필터링
        List<SearchResult> filtered = rawResults.stream()
            .filter(r -> r.score() >= minSimilarityScore)
            .collect(Collectors.toList());
        
        // 2. 중복 제거 (동일 문서 ID)
        List<SearchResult> deduplicated = removeDuplicates(filtered);
        
        // 3. 관련성 순으로 정렬
        List<SearchResult> sorted = deduplicated.stream()
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .collect(Collectors.toList());
        
        // 4. RefinedResult로 변환
        return sorted.stream()
            .map(this::toRefinedResult)
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
    
    /**
     * RefinedResult로 변환
     */
    private RefinedResult toRefinedResult(SearchResult result) {
        return RefinedResult.builder()
            .documentId(result.documentId())
            .text(result.text())
            .score(result.score())
            .collectionType(result.collectionType())
            .metadata(result.metadata())
            .build();
    }
}
