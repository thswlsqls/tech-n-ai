package com.tech.n.ai.datasource.mongodb.util;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Atlas Vector Search 유틸리티
 * 
 * $vectorSearch aggregation pipeline 생성을 위한 유틸리티 클래스.
 * 실제 실행은 api/chatbot 모듈에서 MongoTemplate을 사용하여 수행합니다.
 * 
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
 */
public final class VectorSearchUtil {
    
    // 기본 컬렉션명
    public static final String COLLECTION_CONTESTS = "contests";
    public static final String COLLECTION_NEWS_ARTICLES = "news_articles";
    public static final String COLLECTION_ARCHIVES = "archives";
    
    // 기본 Vector Index 이름
    public static final String INDEX_CONTESTS = "vector_index_contests";
    public static final String INDEX_NEWS_ARTICLES = "vector_index_news_articles";
    public static final String INDEX_ARCHIVES = "vector_index_archives";
    
    private VectorSearchUtil() {
        // 유틸리티 클래스
    }
    
    /**
     * $vectorSearch aggregation stage 생성
     * 
     * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
     * 
     * @param indexName Vector Search Index 이름
     * @param path 벡터 필드 경로 (예: "embedding_vector") - 필수
     * @param queryVector 쿼리 벡터 (1536 dimensions)
     * @param numCandidates 검색 후보 수 (기본값: 100, limit의 10~20배 권장)
     * @param limit 검색 결과 수 제한
     * @param filter 필터 조건 (선택사항, MQL 연산자 사용)
     * @param exact ENN 검색 여부 (기본값: false, ANN 사용)
     * @return $vectorSearch stage Document
     */
    public static Document createVectorSearchStage(
            String indexName,
            String path,
            List<Float> queryVector,
            int numCandidates,
            int limit,
            Document filter,
            boolean exact) {
        
        Document vectorSearchParams = new Document()
            .append("index", indexName)
            .append("path", path)
            .append("queryVector", queryVector)
            .append("limit", limit);
        
        // exact=true일 경우 numCandidates 무시
        if (!exact) {
            vectorSearchParams.append("numCandidates", numCandidates);
        }
        
        // filter가 있으면 추가
        if (filter != null && !filter.isEmpty()) {
            vectorSearchParams.append("filter", filter);
        }
        
        // exact 옵션 추가 (기본값 false이면 생략 가능하지만 명시적으로 추가)
        if (exact) {
            vectorSearchParams.append("exact", true);
        }
        
        return new Document("$vectorSearch", vectorSearchParams);
    }
    
    /**
     * $vectorSearch stage를 VectorSearchOptions로 생성
     * 
     * @param queryVector 쿼리 벡터
     * @param options 검색 옵션
     * @return $vectorSearch stage Document
     */
    public static Document createVectorSearchStage(List<Float> queryVector, VectorSearchOptions options) {
        return createVectorSearchStage(
            options.getIndexName(),
            options.getPath(),
            queryVector,
            options.getNumCandidates(),
            options.getLimit(),
            options.getFilter(),
            options.isExact()
        );
    }
    
    /**
     * Vector Search 결과 프로젝션 stage 생성
     * $meta: "vectorSearchScore"를 포함하여 유사도 점수 추출
     * 
     * @param fields 프로젝션할 필드 목록 (null이면 기본 필드만)
     * @return $project stage Document
     */
    public static Document createProjectionStage(List<String> fields) {
        Document projection = new Document()
            .append("_id", 1)
            .append("score", new Document("$meta", "vectorSearchScore"))
            .append("embedding_text", 1);
        
        if (fields != null) {
            for (String field : fields) {
                projection.append(field, 1);
            }
        }
        
        return new Document("$project", projection);
    }
    
    /**
     * 기본 프로젝션 stage 생성 (모든 필드 + score)
     * 
     * @return $addFields stage Document
     */
    public static Document createScoreAddFieldsStage() {
        return new Document("$addFields", 
            new Document("score", new Document("$meta", "vectorSearchScore")));
    }
    
    /**
     * 유사도 점수 필터링 stage 생성
     * 
     * @param minScore 최소 유사도 점수
     * @return $match stage Document
     */
    public static Document createScoreFilterStage(double minScore) {
        return new Document("$match", 
            new Document("score", new Document("$gte", minScore)));
    }
    
    /**
     * Contest 컬렉션 Vector Search 파이프라인 생성
     * 
     * @param queryVector 쿼리 벡터
     * @param options 검색 옵션
     * @return aggregation pipeline
     */
    public static List<Document> createContestSearchPipeline(
            List<Float> queryVector,
            VectorSearchOptions options) {
        
        List<Document> pipeline = new ArrayList<>();
        
        // 1. $vectorSearch stage
        VectorSearchOptions contestOptions = VectorSearchOptions.builder()
            .indexName(options.getIndexName() != null ? options.getIndexName() : INDEX_CONTESTS)
            .path(options.getPath())
            .numCandidates(options.getNumCandidates())
            .limit(options.getLimit())
            .minScore(options.getMinScore())
            .filter(options.getFilter())
            .exact(options.isExact())
            .build();
        
        pipeline.add(createVectorSearchStage(queryVector, contestOptions));
        
        // 2. $addFields stage (score 추가)
        pipeline.add(createScoreAddFieldsStage());
        
        // 3. $match stage (minScore 필터링)
        if (options.getMinScore() > 0) {
            pipeline.add(createScoreFilterStage(options.getMinScore()));
        }
        
        return pipeline;
    }
    
    /**
     * NewsArticle 컬렉션 Vector Search 파이프라인 생성
     * 
     * @param queryVector 쿼리 벡터
     * @param options 검색 옵션
     * @return aggregation pipeline
     */
    public static List<Document> createNewsArticleSearchPipeline(
            List<Float> queryVector,
            VectorSearchOptions options) {
        
        List<Document> pipeline = new ArrayList<>();
        
        // 1. $vectorSearch stage
        VectorSearchOptions newsOptions = VectorSearchOptions.builder()
            .indexName(options.getIndexName() != null ? options.getIndexName() : INDEX_NEWS_ARTICLES)
            .path(options.getPath())
            .numCandidates(options.getNumCandidates())
            .limit(options.getLimit())
            .minScore(options.getMinScore())
            .filter(options.getFilter())
            .exact(options.isExact())
            .build();
        
        pipeline.add(createVectorSearchStage(queryVector, newsOptions));
        
        // 2. $addFields stage (score 추가)
        pipeline.add(createScoreAddFieldsStage());
        
        // 3. $match stage (minScore 필터링)
        if (options.getMinScore() > 0) {
            pipeline.add(createScoreFilterStage(options.getMinScore()));
        }
        
        return pipeline;
    }
    
    /**
     * Archive 컬렉션 Vector Search 파이프라인 생성 (userId 필터 포함)
     * 
     * @param queryVector 쿼리 벡터
     * @param userId 사용자 ID (필터링용)
     * @param options 검색 옵션
     * @return aggregation pipeline
     */
    public static List<Document> createArchiveSearchPipeline(
            List<Float> queryVector,
            String userId,
            VectorSearchOptions options) {
        
        List<Document> pipeline = new ArrayList<>();
        
        // userId 필터 생성 (기존 filter와 병합)
        Document userFilter = new Document("user_id", userId);
        Document combinedFilter;
        
        if (options.getFilter() != null && !options.getFilter().isEmpty()) {
            combinedFilter = new Document("$and", List.of(userFilter, options.getFilter()));
        } else {
            combinedFilter = userFilter;
        }
        
        // 1. $vectorSearch stage
        VectorSearchOptions archiveOptions = VectorSearchOptions.builder()
            .indexName(options.getIndexName() != null ? options.getIndexName() : INDEX_ARCHIVES)
            .path(options.getPath())
            .numCandidates(options.getNumCandidates())
            .limit(options.getLimit())
            .minScore(options.getMinScore())
            .filter(combinedFilter)
            .exact(options.isExact())
            .build();
        
        pipeline.add(createVectorSearchStage(queryVector, archiveOptions));
        
        // 2. $addFields stage (score 추가)
        pipeline.add(createScoreAddFieldsStage());
        
        // 3. $match stage (minScore 필터링)
        if (options.getMinScore() > 0) {
            pipeline.add(createScoreFilterStage(options.getMinScore()));
        }
        
        return pipeline;
    }
}
