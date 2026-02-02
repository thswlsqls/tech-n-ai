package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Cohere Re-Ranking 서비스 구현체
 */
@Slf4j
@Service
public class CohereReRankingServiceImpl implements ReRankingService {

    @Value("${chatbot.reranking.enabled:false}")
    private boolean enabled;

    @Value("${chatbot.reranking.api-key:}")
    private String apiKey;

    @Value("${chatbot.reranking.model-name:rerank-multilingual-v3.0}")
    private String modelName;

    @Value("${chatbot.reranking.min-score:0.3}")
    private double minScore;

    private ScoringModel scoringModel;

    @PostConstruct
    public void init() {
        if (enabled && !apiKey.isBlank()) {
            try {
                this.scoringModel = CohereScoringModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .build();
                log.info("Cohere Re-Ranking initialized with model: {}", modelName);
            } catch (Exception e) {
                log.error("Failed to initialize Cohere Re-Ranking", e);
                this.scoringModel = null;
            }
        } else {
            log.info("Cohere Re-Ranking is disabled");
        }
    }

    @Override
    public List<SearchResult> rerank(String query, List<SearchResult> documents, int topK) {
        if (!isEnabled() || documents.isEmpty()) {
            log.info("Re-Ranking skipped: enabled={}, documents={}", isEnabled(), documents.size());
            return fallbackSort(documents, topK);
        }

        try {
            // SearchResult -> TextSegment 변환
            List<TextSegment> segments = documents.stream()
                .map(doc -> TextSegment.from(doc.text()))
                .toList();

            // Cohere Scoring
            Response<List<Double>> response = scoringModel.scoreAll(segments, query);
            List<Double> scores = response.content();

            // 점수 매핑 및 정렬
            List<SearchResult> rerankedResults = IntStream.range(0, documents.size())
                .mapToObj(i -> {
                    SearchResult original = documents.get(i);
                    double newScore = scores.get(i);
                    return SearchResult.builder()
                        .documentId(original.documentId())
                        .text(original.text())
                        .score(newScore)
                        .collectionType(original.collectionType())
                        .metadata(original.metadata())
                        .build();
                })
                .filter(doc -> doc.score() >= minScore)
                .sorted(Comparator.comparing(SearchResult::score).reversed())
                .limit(topK)
                .toList();

            log.info("Re-Ranking: {} -> {} documents", documents.size(), rerankedResults.size());
            return rerankedResults;

        } catch (Exception e) {
            log.error("Re-Ranking failed, using fallback", e);
            return fallbackSort(documents, topK);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && scoringModel != null;
    }

    private List<SearchResult> fallbackSort(List<SearchResult> documents, int topK) {
        return documents.stream()
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .limit(topK)
            .toList();
    }
}
