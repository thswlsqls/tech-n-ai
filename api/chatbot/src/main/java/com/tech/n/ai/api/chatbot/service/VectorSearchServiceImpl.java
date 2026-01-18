package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchOptions;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import com.tech.n.ai.datasource.mongodb.document.ArchiveDocument;
import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import com.tech.n.ai.datasource.mongodb.document.NewsArticleDocument;
import com.tech.n.ai.datasource.mongodb.repository.ArchiveRepository;
import com.tech.n.ai.datasource.mongodb.repository.ContestRepository;
import com.tech.n.ai.datasource.mongodb.repository.NewsArticleRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 벡터 검색 서비스 구현체
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
        
        if (Boolean.TRUE.equals(options.includeContests())) {
            results.addAll(searchContests(queryVector, options));
        }
        if (Boolean.TRUE.equals(options.includeNews())) {
            results.addAll(searchNews(queryVector, options));
        }
        if (Boolean.TRUE.equals(options.includeArchives()) && userId != null) {
            results.addAll(searchArchives(queryVector, userId.toString(), options));
        }
        
        // 3. 유사도 점수로 정렬 및 필터링
        return results.stream()
            .filter(r -> r.score() >= options.minSimilarityScore())
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(options.maxResults())
            .collect(Collectors.toList());
    }
    
    /**
     * Contest 벡터 검색
     * 
     * TODO: MongoDB Atlas Vector Search 구현 필요
     * - $vectorSearch aggregation stage 사용
     * - Spring Data MongoDB의 VectorSearchOperation 사용 (Spring Data MongoDB 4.5.0+)
     * - 또는 MongoTemplate.executeCommand()를 사용하여 직접 BSON 문서로 작성
     */
    private List<SearchResult> searchContests(List<Float> queryVector, SearchOptions options) {
        // TODO: MongoDB Atlas Vector Search 구현
        // 현재는 빈 리스트 반환 (실제 구현 시 VectorSearchOperation 또는 직접 BSON 문서 사용)
        log.warn("Vector search for contests not yet implemented");
        return new ArrayList<>();
    }
    
    /**
     * News 벡터 검색
     * 
     * TODO: MongoDB Atlas Vector Search 구현 필요
     */
    private List<SearchResult> searchNews(List<Float> queryVector, SearchOptions options) {
        // TODO: MongoDB Atlas Vector Search 구현
        log.warn("Vector search for news not yet implemented");
        return new ArrayList<>();
    }
    
    /**
     * Archive 벡터 검색 (userId 필터링 포함)
     * 
     * TODO: MongoDB Atlas Vector Search 구현 필요
     */
    private List<SearchResult> searchArchives(List<Float> queryVector, String userId, SearchOptions options) {
        // TODO: MongoDB Atlas Vector Search 구현
        log.warn("Vector search for archives not yet implemented");
        return new ArrayList<>();
    }
}
