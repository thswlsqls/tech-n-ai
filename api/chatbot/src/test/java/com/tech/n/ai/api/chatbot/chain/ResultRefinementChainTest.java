package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.ReRankingService;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResultRefinementChain 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResultRefinementChain 단위 테스트")
class ResultRefinementChainTest {

    @Mock
    private ReRankingService reRankingService;

    @InjectMocks
    private ResultRefinementChain refinementChain;

    // ========== refine 테스트 ==========

    @Nested
    @DisplayName("refine - Re-Ranking 비활성화")
    class RefineWithoutReRanking {

        @Test
        @DisplayName("Re-Ranking 비활성화 시 score 기준 정렬")
        void refine_scoreBasedSorting() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.6),
                createSearchResult("doc2", 0.9),
                createSearchResult("doc3", 0.75)
            );

            // When
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).documentId()).isEqualTo("doc2"); // 0.9
            assertThat(result.get(1).documentId()).isEqualTo("doc3"); // 0.75
            assertThat(result.get(2).documentId()).isEqualTo("doc1"); // 0.6
        }

        @Test
        @DisplayName("maxSearchResults 제한 적용")
        void refine_maxResultsLimit() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 2);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.9),
                createSearchResult("doc2", 0.8),
                createSearchResult("doc3", 0.7)
            );

            // When
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    // ========== Re-Ranking 활성화 테스트 ==========

    @Nested
    @DisplayName("refine - Re-Ranking 활성화")
    class RefineWithReRanking {

        @Test
        @DisplayName("Re-Ranking 서비스 호출")
        void refine_callsReRankingService() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(true);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.6),
                createSearchResult("doc2", 0.9)
            );

            List<SearchResult> rerankedResults = List.of(
                createSearchResult("doc2", 0.95),
                createSearchResult("doc1", 0.65)
            );
            when(reRankingService.rerank(anyString(), anyList(), anyInt()))
                .thenReturn(rerankedResults);

            // When
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then
            verify(reRankingService).rerank(eq("테스트 쿼리"), anyList(), eq(10));
            assertThat(result).hasSize(2);
        }
    }

    // ========== 중복 제거 테스트 ==========

    @Nested
    @DisplayName("refine - 중복 제거")
    class Deduplication {

        @Test
        @DisplayName("동일 documentId 중복 제거")
        void refine_removeDuplicates() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.9),
                createSearchResult("doc1", 0.85), // 중복
                createSearchResult("doc2", 0.8)
            );

            // When
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.stream().map(SearchResult::documentId))
                .containsExactlyInAnyOrder("doc1", "doc2");
        }

        @Test
        @DisplayName("중복 제거 시 첫 번째 결과 유지")
        void refine_keepFirstDuplicate() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.9),
                createSearchResult("doc1", 0.7) // 중복 (낮은 점수)
            );

            // When
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then: recency boost 적용으로 score가 변경됨 (published_at 없으면 recencyScore=0.5)
            // hybridScore = 0.9 * 0.85 + 0.5 * 0.15 = 0.84
            assertThat(result).hasSize(1);
            assertThat(result.get(0).documentId()).isEqualTo("doc1");
        }
    }

    // ========== Score Fusion 적용 시 Recency Boost 스킵 ==========

    @Nested
    @DisplayName("refine - scoreFusionApplied=true")
    class ScoreFusionApplied {

        @Test
        @DisplayName("Score Fusion 적용 시 Recency Boost 스킵 (score 변경 없음)")
        void refine_scoreFusionApplied_skipsRecencyBoost() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.92),
                createSearchResult("doc2", 0.88)
            );

            // When
            List<SearchResult> result = refinementChain.refine(
                "테스트 쿼리", rawResults, false, true);

            // Then: score가 Recency Boost로 변경되지 않음
            assertThat(result).hasSize(2);
            assertThat(result.get(0).score()).isEqualTo(0.92);
            assertThat(result.get(1).score()).isEqualTo(0.88);
        }

        @Test
        @DisplayName("Score Fusion 미적용 시 Recency Boost 적용 (score 변경됨)")
        void refine_scoreFusionNotApplied_appliesRecencyBoost() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.92)
            );

            // When
            List<SearchResult> result = refinementChain.refine(
                "테스트 쿼리", rawResults, false, false);

            // Then: Recency Boost 적용으로 score 변경 (published_at 없음 → recency=0.5)
            // hybridScore = 0.92 * 0.85 + 0.5 * 0.15 = 0.857
            assertThat(result.get(0).score()).isNotEqualTo(0.92);
        }

        @Test
        @DisplayName("Score Fusion + Re-Ranking 활성화 시 Re-Rank 수행, Recency Boost 스킵")
        void refine_scoreFusionWithReRanking_reranksButSkipsRecencyBoost() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(true);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.92),
                createSearchResult("doc2", 0.88)
            );

            List<SearchResult> rerankedResults = List.of(
                createSearchResult("doc2", 0.95),
                createSearchResult("doc1", 0.90)
            );
            when(reRankingService.rerank(anyString(), anyList(), anyInt()))
                .thenReturn(rerankedResults);

            // When
            List<SearchResult> result = refinementChain.refine(
                "테스트 쿼리", rawResults, false, true);

            // Then: Re-Ranking 호출됨
            verify(reRankingService).rerank(anyString(), anyList(), anyInt());
            // Re-Ranking 결과가 Recency Boost 없이 score 기준 정렬
            assertThat(result.get(0).score()).isEqualTo(0.95);
        }

        @Test
        @DisplayName("2인자 refine은 scoreFusionApplied=false 기본값")
        void refine_twoArgOverload_defaultsFalse() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.92)
            );

            // When: 2인자 오버로드 → recencyDetected=false, scoreFusionApplied=false
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then: Recency Boost 적용됨 (score 변경)
            assertThat(result.get(0).score()).isNotEqualTo(0.92);
        }

        @Test
        @DisplayName("Score Fusion + maxSearchResults 제한")
        void refine_scoreFusionApplied_limitsResults() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 2);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult("doc1", 0.92),
                createSearchResult("doc2", 0.88),
                createSearchResult("doc3", 0.85)
            );

            // When
            List<SearchResult> result = refinementChain.refine(
                "테스트 쿼리", rawResults, false, true);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).documentId()).isEqualTo("doc1");
        }
    }

    // ========== 엣지 케이스 ==========

    @Nested
    @DisplayName("refine - 엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("빈 결과 처리")
        void refine_emptyResults() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of();

            // When
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null documentId 처리")
        void refine_nullDocumentId() {
            // Given
            ReflectionTestUtils.setField(refinementChain, "maxSearchResults", 5);
            when(reRankingService.isEnabled()).thenReturn(false);

            List<SearchResult> rawResults = List.of(
                createSearchResult(null, 0.9),
                createSearchResult("doc1", 0.8)
            );

            // When
            List<SearchResult> result = refinementChain.refine("테스트 쿼리", rawResults);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    // ========== 헬퍼 메서드 ==========

    private SearchResult createSearchResult(String documentId, double score) {
        return SearchResult.builder()
            .documentId(documentId)
            .text("테스트 텍스트")
            .score(score)
            .collectionType("EMERGING_TECH")
            .build();
    }
}
