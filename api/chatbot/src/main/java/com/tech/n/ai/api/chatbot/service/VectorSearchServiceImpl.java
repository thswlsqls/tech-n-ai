package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchOptions;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import com.tech.n.ai.domain.mongodb.util.VectorSearchOptions;
import com.tech.n.ai.domain.mongodb.util.VectorSearchUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 벡터 검색 서비스 구현체
 *
 * MongoDB Atlas Vector Search를 사용하여 벡터 검색을 수행합니다.
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {

    private final EmbeddingModel embeddingModel;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<SearchResult> search(String query, Long userId, SearchOptions options) {
        // 1. 쿼리 임베딩 생성 (OpenAI text-embedding-3-small은 document/query 구분 없음)
        Embedding embedding = embeddingModel.embed(query).content();
        List<Float> queryVector = embedding.vectorAsList();

        // 2. 컬렉션별 검색
        List<SearchResult> results = new ArrayList<>();

        if (Boolean.TRUE.equals(options.includeBookmarks()) && userId != null) {
            results.addAll(searchBookmarks(queryVector, userId.toString(), options));
        }

        // 3. 유사도 점수로 정렬 및 최종 결과 수 제한
        return results.stream()
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(options.maxResults())
            .collect(Collectors.toList());
    }

    /**
     * Bookmark 벡터 검색 (userId 필터링 포함)
     *
     * MongoDB Atlas Vector Search의 $vectorSearch aggregation stage를 사용합니다.
     * userId로 pre-filter를 적용합니다.
     */
    private List<SearchResult> searchBookmarks(List<Float> queryVector, String userId, SearchOptions options) {
        try {
            // 1. VectorSearchOptions 생성
            VectorSearchOptions vectorOptions = VectorSearchOptions.builder()
                .indexName(VectorSearchUtil.INDEX_BOOKMARKS)
                .numCandidates(options.numCandidates() != null ? options.numCandidates() : 100)
                .limit(options.maxResults() != null ? options.maxResults() : 5)
                .minScore(options.minSimilarityScore() != null ? options.minSimilarityScore() : 0.7)
                .exact(Boolean.TRUE.equals(options.exact()))
                .build();

            // 2. aggregation pipeline 생성 (userId 필터 포함)
            List<Document> pipeline = VectorSearchUtil.createBookmarkSearchPipeline(queryVector, userId, vectorOptions);

            // 3. 실행
            List<Document> results = mongoTemplate.getCollection(VectorSearchUtil.COLLECTION_BOOKMARKS)
                .aggregate(pipeline)
                .into(new ArrayList<>());

            // 4. SearchResult로 변환
            return results.stream()
                .map(doc -> convertToSearchResult(doc, "BOOKMARK"))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Vector search for bookmarks failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * MongoDB Document를 SearchResult로 변환
     *
     * @param doc MongoDB Document
     * @param collectionType 컬렉션 타입 (BOOKMARK)
     * @return SearchResult
     */
    private SearchResult convertToSearchResult(Document doc, String collectionType) {
        return SearchResult.builder()
            .documentId(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toString() : null)
            .text(doc.getString("embedding_text"))
            .score(doc.getDouble("score") != null ? doc.getDouble("score") : 0.0)
            .collectionType(collectionType)
            .metadata(doc)
            .build();
    }
}
