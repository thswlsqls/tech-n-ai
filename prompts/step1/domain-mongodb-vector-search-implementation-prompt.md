# MongoDB Atlas Vector Search 구현 보완 프롬프트

**대상 모듈**: `domain/mongodb`, `api/chatbot` 
**목적**: MongoDB Atlas Vector Search 미구현/부족 항목 보완 
**작성 일시**: 2026-01-16 (수정: 2026-01-19) 

---

## 배경 및 현재 상태

### 현재 구현 상태

**준수 항목**:
- ✅ Document 클래스에 `embedding_text`, `embedding_vector` 필드 정의 완료
- ✅ 벡터 타입: `List<Float>` (1536 dimensions)
- ✅ `ContestDocument`, `NewsArticleDocument`, `BookmarkDocument` 모두 벡터 필드 포함

**미구현/부족 항목**:
- ❌ Vector Search Index 생성 코드 없음 (`domain/mongodb`)
- ❌ `$vectorSearch` aggregation 실행 유틸리티 없음 (`domain/mongodb`)
- ❌ Vector Search 쿼리 파라미터 (`numCandidates`, `limit`, `filter`) 설정 유틸리티 없음 (`domain/mongodb`)
- ❌ `$meta: "vectorSearchScore"` 유사도 점수 추출 로직 없음 (`domain/mongodb`)
- ❌ `VectorSearchServiceImpl` 실제 구현 없음 - TODO 상태 (`api/chatbot`)
- ❌ `SearchOptions`에 `numCandidates`, `exact` 옵션 없음 (`api/chatbot`)

### 참고 자료

**공식 문서** (반드시 참고):
- [MongoDB Atlas Vector Search 공식 문서](https://www.mongodb.com/products/platform/atlas-vector-search)
- [Atlas Vector Search 가이드](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [$vectorSearch aggregation stage](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/)
- [Vector Search Index 생성](https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/)

**프로젝트 내 참고 문서**:
- `docs/step12/rag-chatbot-design.md`: Vector Search 설계 및 예제
- `api/chatbot/src/main/java/com/ebson/shrimp/tm/demo/api/chatbot/service/VectorSearchServiceImpl.java`: 실제 사용 패턴

---

## 구현 요구사항

### 1. Vector Search Index 생성 가이드

**주의**: Vector Search Index는 MongoDB Atlas에서만 생성 가능합니다. Spring Data MongoDB로는 생성할 수 없으므로, 생성 스크립트와 가이드만 제공합니다.

#### 1.1 Vector Search Index 생성 스크립트 클래스

**파일 위치**: `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/config/VectorSearchIndexConfig.java`

**역할**: Vector Search Index 생성 스크립트를 제공하는 유틸리티 클래스

**구현 내용**:
- 각 컬렉션별 Vector Search Index 정의 JSON 반환 메서드
- Index 생성 가이드 메서드 (로깅용)
- 실제 Index 생성은 Atlas UI/CLI에서 수동 수행

**주의사항**:
- `MongoIndexConfig`와는 별도 클래스로 분리 (Vector Search Index는 코드로 생성 불가)
- Index 정의만 제공하고, 실제 생성은 가이드 문서 참조

#### 1.2 Index 정의 예제

**공식 문서**: https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/

**Vector Index 필드 옵션**:
- `type`: `"vector"` (벡터 필드) 또는 `"filter"` (필터 필드)
- `path`: 필드 경로
- `numDimensions`: 벡터 차원 수 (1536 for OpenAI text-embedding-3-small)
- `similarity`: 유사도 함수 - `"cosine"`, `"euclidean"`, `"dotProduct"` 중 선택
  - `cosine`: 정규화된 벡터에 적합 (기본 권장)
  - `euclidean`: 유클리드 거리 기반
  - `dotProduct`: 내적 기반, 정규화된 벡터에서 cosine과 동일

**ContestDocument Vector Index**:
```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding_vector",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "status"
    }
  ]
}
```

**NewsArticleDocument Vector Index**:
```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding_vector",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "published_at"
    }
  ]
}
```

**BookmarkDocument Vector Index**:
```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding_vector",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "user_id"
    }
  ]
}
```

### 2. Vector Search 쿼리 유틸리티 클래스

**파일 위치**: `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/util/VectorSearchUtil.java`

**역할**: `$vectorSearch` aggregation pipeline 생성 유틸리티

**구현 내용**:
- `$vectorSearch` stage 생성 메서드
- `numCandidates`, `limit`, `filter` 파라미터 설정
- `$meta: "vectorSearchScore"` 프로젝션 포함
- 컬렉션별 검색 파이프라인 빌더 메서드

**공식 문서 준수**:
- `$vectorSearch` aggregation stage 사용 (deprecated `knnBeta` 사용 금지)
- `numCandidates`: 기본값 100 (성능과 정확도 균형)
- `limit`: 검색 결과 수 제한
- `filter`: 메타데이터 필터링 (status, userId 등)

### 3. Vector Search Repository 확장 (선택사항)

**파일 위치**: 각 Repository 인터페이스에 메서드 추가 또는 별도 `VectorSearchRepository` 인터페이스

**역할**: Vector Search 쿼리 실행을 위한 Repository 메서드

**구현 방식**:
- `MongoTemplate.aggregate()` 사용
- `VectorSearchUtil`을 활용한 파이프라인 생성
- 반환 타입: `List<Document>` 또는 커스텀 DTO

**주의사항**:
- `domain/mongodb` 모듈은 도메인 레이어이므로, 실제 Vector Search 실행은 `api/chatbot` 모듈에서 수행
- `domain/mongodb` 모듈에는 유틸리티와 기본 구조만 제공

---

## 상세 구현 가이드

### 1. VectorSearchIndexConfig 클래스 구현

**클래스 구조**:
```java
package com.ebson.shrimp.tm.demo.domain.mongodb.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Vector Search Index 설정 유틸리티
 * 
 * 참고: Vector Search Index는 MongoDB Atlas에서만 생성 가능합니다.
 * 이 클래스는 Index 정의만 제공하며, 실제 생성은 Atlas UI/CLI에서 수행해야 합니다.
 * 
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/
 */
@Slf4j
@Configuration
public class VectorSearchIndexConfig {
    
    /**
     * ContestDocument Vector Search Index 정의
     * 
     * @return Index 정의 JSON 문자열
     */
    public static String getContestVectorIndexDefinition() {
        return """
            {
              "fields": [
                {
                  "type": "vector",
                  "path": "embedding_vector",
                  "numDimensions": 1536,
                  "similarity": "cosine"
                },
                {
                  "type": "filter",
                  "path": "status"
                }
              ]
            }
            """;
    }
    
    /**
     * NewsArticleDocument Vector Search Index 정의
     */
    public static String getNewsArticleVectorIndexDefinition() {
        // 구현
    }
    
    /**
     * BookmarkDocument Vector Search Index 정의
     */
    public static String getBookmarkVectorIndexDefinition() {
        // 구현
    }
    
    /**
     * Index 생성 가이드 로깅
     * 애플리케이션 시작 시 Vector Search Index 생성 필요 여부 안내
     */
    @PostConstruct
    public void logIndexCreationGuide() {
        log.info("=== MongoDB Atlas Vector Search Index 생성 가이드 ===");
        log.info("Vector Search Index는 MongoDB Atlas에서만 생성 가능합니다.");
        log.info("Atlas UI 또는 Atlas CLI를 사용하여 Index를 생성하세요.");
        log.info("가이드 문서: docs/step1/6. mongodb-atlas-integration-guide.md");
        log.info("==================================================");
    }
}
```

### 2. VectorSearchUtil 클래스 구현

**클래스 구조**:
```java
package com.ebson.shrimp.tm.demo.domain.mongodb.util;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB Atlas Vector Search 유틸리티
 * 
 * $vectorSearch aggregation pipeline 생성 및 실행을 위한 유틸리티 클래스
 * 
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
 */
public class VectorSearchUtil {
    
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
        // 구현 예시:
        // Document vectorSearchStage = new Document("$vectorSearch", 
        //     new Document("index", indexName)
        //         .append("path", path)
        //         .append("queryVector", queryVector)
        //         .append("numCandidates", numCandidates)
        //         .append("limit", limit)
        //         .append("filter", filter)
        //         .append("exact", exact));
    }
    
    /**
     * Vector Search 결과 프로젝션 stage 생성
     * $meta: "vectorSearchScore"를 포함하여 유사도 점수 추출
     * 
     * @param fields 프로젝션할 필드 목록
     * @return $project stage Document
     */
    public static Document createProjectionStage(List<String> fields) {
        // 구현
    }
    
    /**
     * 유사도 점수 필터링 stage 생성
     * 
     * @param minScore 최소 유사도 점수
     * @return $match stage Document
     */
    public static Document createScoreFilterStage(double minScore) {
        // 구현
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
        // 구현
    }
    
    /**
     * NewsArticle 컬렉션 Vector Search 파이프라인 생성
     */
    public static List<Document> createNewsArticleSearchPipeline(
            List<Float> queryVector,
            VectorSearchOptions options) {
        // 구현
    }
    
    /**
     * Bookmark 컬렉션 Vector Search 파이프라인 생성 (userId 필터 포함)
     */
    public static List<Document> createBookmarkSearchPipeline(
            List<Float> queryVector,
            String userId,
            VectorSearchOptions options) {
        // 구현
    }
}
```

**VectorSearchOptions 클래스**:
```java
package com.ebson.shrimp.tm.demo.domain.mongodb.util;

import lombok.Builder;
import lombok.Value;
import org.bson.Document;

/**
 * Vector Search 옵션
 * 
 * 공식 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
 */
@Value
@Builder
public class VectorSearchOptions {
    String indexName;       // Vector Search Index 이름
    String path;            // 벡터 필드 경로 (기본값: "embedding_vector")
    int numCandidates;      // 검색 후보 수 (기본값: 100, limit의 10~20배 권장)
    int limit;              // 검색 결과 수 제한 (기본값: 5)
    double minScore;        // 최소 유사도 점수 (기본값: 0.7)
    Document filter;        // 필터 조건 (선택사항, MQL 연산자 사용)
    boolean exact;          // ENN 검색 여부 (기본값: false, ANN 사용)
    
    // 기본값 빌더
    public static class VectorSearchOptionsBuilder {
        private String path = "embedding_vector";
        private int numCandidates = 100;
        private int limit = 5;
        private double minScore = 0.7;
        private boolean exact = false;
    }
}
```

### 3. 공식 문서 준수 사항

#### 3.1 $vectorSearch 파라미터

**공식 문서**: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/

**필수 파라미터**:
- `index`: Vector Search Index 이름 (문자열)
- `path`: 벡터 필드 경로 (`embedding_vector`) - **반드시 포함**
- `queryVector`: 쿼리 벡터 배열 (1536 dimensions, `List<Float>` 또는 `List<Double>`)

**선택 파라미터**:
- `numCandidates`: 검색 후보 수 (기본값: 100, 권장: limit의 10~20배)
- `limit`: 검색 결과 수 제한 (기본값: 5)
- `filter`: 메타데이터 필터링 조건 (MQL 연산자: `$eq`, `$in`, `$gte`, `$lte` 등)
- `exact`: ENN(Exact Nearest Neighbor) 검색 활성화 (기본값: false, ANN 사용)

**`exact` 옵션 (ANN vs ENN)**:
- `exact: false` (기본값): ANN(Approximate Nearest Neighbor) - 빠르지만 근사치
- `exact: true`: ENN(Exact Nearest Neighbor) - 정확하지만 느림, 소규모 데이터에 적합
- ENN 사용 시 `numCandidates` 무시됨

#### 3.2 $meta: "vectorSearchScore"

**사용법**:
```javascript
{
  $project: {
    score: { $meta: "vectorSearchScore" }
  }
}
```

**역할**: `$vectorSearch` stage에서 계산된 유사도 점수를 추출

**공식 문서 참조**: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/#vectorsearchscore-metadata

#### 3.3 deprecated operator 사용 금지

**사용 금지**:
- `knnBeta` operator (deprecated)
- `knnVector` field type (deprecated)

**사용 필수**:
- `$vectorSearch` aggregation stage (최신)

**공식 문서 참조**: https://www.mongodb.com/products/platform/atlas-vector-search

#### 3.4 Filter 연산자 (Pre-filter)

**공식 문서**: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/#atlas-vector-search-pre-filter

**지원되는 MQL 연산자**:
- 비교: `$eq`, `$ne`, `$gt`, `$gte`, `$lt`, `$lte`
- 배열: `$in`, `$nin`
- 논리: `$and`, `$or`, `$not`

**사용 예시**:
```java
// 단일 조건
Document filter = new Document("status", "ACTIVE");

// 복합 조건
Document filter = new Document("$and", List.of(
    new Document("status", new Document("$in", List.of("UPCOMING", "ONGOING"))),
    new Document("end_date", new Document("$gte", LocalDateTime.now()))
));
```

**주의사항**:
- 필터 필드는 Vector Search Index에 `type: "filter"`로 정의되어 있어야 함
- 필터링은 벡터 검색 전에 수행됨 (pre-filter)

### 4. 구현 패턴

#### 4.1 MongoTemplate을 사용한 Aggregation 실행

**패턴**:
```java
List<Document> pipeline = VectorSearchUtil.createContestSearchPipeline(
    queryVector, options);

List<Document> results = mongoTemplate.aggregate(
    pipeline, 
    "contests", 
    Document.class
).getMappedResults();
```

#### 4.2 유사도 점수 추출

**패턴**:
```java
for (Document result : results) {
    Double score = result.getDouble("score");
    // score는 $meta: "vectorSearchScore"로 추출된 값
}
```

#### 4.3 필터 조건 활용

**패턴**:
```java
Document filter = new Document("status", 
    new Document("$in", List.of("UPCOMING", "ONGOING")));

VectorSearchOptions options = VectorSearchOptions.builder()
    .indexName("vector_index_contests")
    .numCandidates(100)
    .limit(5)
    .minScore(0.7)
    .filter(filter)
    .build();
```

---

## 구현 범위 및 제외 사항

### 포함 사항

1. **VectorSearchIndexConfig**: Index 정의 제공 및 가이드 로깅
2. **VectorSearchUtil**: `$vectorSearch` pipeline 생성 유틸리티
3. **VectorSearchOptions**: 검색 옵션 DTO
4. **기본 검색 파이프라인**: Contest, NewsArticle, Bookmark 컬렉션용

### 제외 사항

1. **실제 Vector Search 실행**: `api/chatbot` 모듈에서 수행
2. **전용 Search Nodes 구성**: 인프라 설정 (코드 범위 밖)
3. **Quantization**: 선택사항 (현재는 full-fidelity 벡터만 사용)
4. **인덱스 크기 모니터링**: 운영 모니터링 도구 범위
5. **Vector Search Index 자동 생성**: Atlas에서만 생성 가능 (코드로 불가)

---

## 구현 원칙

### 1. 공식 문서 준수

- 모든 구현은 MongoDB Atlas Vector Search 공식 문서 기반
- deprecated operator (`knnBeta`) 사용 금지
- `$vectorSearch` aggregation stage만 사용

### 2. 최소 구현 원칙

- 현재 프로젝트에 필요한 기능만 구현
- 오버엔지니어링 지양
- 단순하고 명확한 구조

### 3. 도메인 레이어 역할

- `domain/mongodb` 모듈은 도메인 레이어
- Vector Search 실행은 `api/chatbot` 모듈에서 수행
- `domain/mongodb` 모듈은 유틸리티와 기본 구조만 제공

### 4. 기존 코드 패턴 준수

- 프로젝트의 기존 코드 스타일 유지
- `MongoIndexConfig` 패턴 참고
- Lombok 사용 (`@Slf4j`, `@Value`, `@Builder` 등)

---

## 검증 기준

### 1. 공식 문서 준수 검증

- [ ] `$vectorSearch` aggregation stage 사용
- [ ] `knnBeta` operator 미사용 (deprecated)
- [ ] `index`, `path`, `queryVector` 필수 파라미터 포함
- [ ] `numCandidates` 파라미터 설정 (기본값: 100, limit의 10~20배)
- [ ] `$meta: "vectorSearchScore"` 사용
- [ ] `filter` 파라미터 활용 (pre-filter, MQL 연산자)
- [ ] `exact` 옵션 지원 (ANN vs ENN 선택)
- [ ] Vector Index `similarity` 옵션 올바르게 설정 (cosine/euclidean/dotProduct)

### 2. 코드 품질 검증

- [ ] 컴파일 에러 없음
- [ ] 기존 코드와 일관성 유지
- [ ] 적절한 주석 및 JavaDoc
- [ ] 예외 처리 포함

### 3. 기능 검증

- [ ] Vector Search Index 정의 정확성
- [ ] 파이프라인 생성 메서드 정확성
- [ ] 유사도 점수 추출 로직 정확성
- [ ] 필터 조건 적용 정확성

### 4. api/chatbot 모듈 연동 검증

- [ ] `SearchOptions`에 `numCandidates`, `exact` 필드 추가
- [ ] `VectorSearchServiceImpl`에서 `VectorSearchUtil` 활용
- [ ] `MongoTemplate.aggregate()` 또는 `getCollection().aggregate()` 사용
- [ ] Contest, News, Bookmark 컬렉션별 검색 정상 동작
- [ ] userId 필터 적용 (Bookmark 검색)

---

## 참고 자료

### MongoDB Atlas 공식 문서

1. **Vector Search 개요**
   - https://www.mongodb.com/products/platform/atlas-vector-search
   - https://www.mongodb.com/docs/atlas/atlas-vector-search/

2. **$vectorSearch aggregation stage**
   - https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/

3. **Vector Search Index 생성**
   - https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/

4. **성능 최적화**
   - https://www.mongodb.com/docs/atlas/atlas-vector-search/performance/

### 프로젝트 내 참고 문서

- `docs/step12/rag-chatbot-design.md`: Vector Search 설계 및 예제
- `docs/step1/6. mongodb-atlas-integration-guide.md`: MongoDB Atlas 통합 가이드

---

## 구현 순서

1. **VectorSearchIndexConfig 클래스 생성**
   - Index 정의 메서드 구현
   - 가이드 로깅 메서드 구현

2. **VectorSearchOptions 클래스 생성**
   - 검색 옵션 DTO 구현

3. **VectorSearchUtil 클래스 생성**
   - `$vectorSearch` stage 생성 메서드
   - 프로젝션 stage 생성 메서드
   - 필터링 stage 생성 메서드
   - 컬렉션별 파이프라인 생성 메서드

4. **테스트 및 검증**
   - 컴파일 에러 확인
   - 공식 문서 준수 확인
   - 기존 코드와의 일관성 확인

---

## api/chatbot 모듈 연동 가이드

### 현재 상태

**파일 위치**: `api/chatbot/src/main/java/com/ebson/shrimp/tm/demo/api/chatbot/service/`

**현재 구현 상태**:
- ❌ `VectorSearchServiceImpl.java`: TODO 상태, 실제 Vector Search 미구현
- ⚠️ `SearchOptions.java`: `numCandidates`, `exact` 옵션 없음
- ✅ `SearchResult.java`: 기본 구조 완료

### 1. SearchOptions 보완

**파일**: `api/chatbot/src/main/java/com/ebson/shrimp/tm/demo/api/chatbot/service/dto/SearchOptions.java`

**추가 필요 필드**:
```java
@Builder
public record SearchOptions(
    Boolean includeContests,      // Contest 포함 여부
    Boolean includeNews,          // News 포함 여부
    Boolean includeBookmarks,      // Bookmark 포함 여부
    Integer maxResults,           // 최대 결과 수 (limit)
    Integer numCandidates,        // 검색 후보 수 (추가)
    Double minSimilarityScore,    // 최소 유사도 점수
    Boolean exact                 // ENN 검색 여부 (추가)
) {
    public static SearchOptions defaults() {
        return SearchOptions.builder()
            .includeContests(true)
            .includeNews(true)
            .includeBookmarks(true)
            .maxResults(5)
            .numCandidates(100)        // 추가: limit의 10~20배 권장
            .minSimilarityScore(0.7)
            .exact(false)              // 추가: ANN 사용 (기본값)
            .build();
    }
}
```

### 2. VectorSearchServiceImpl 구현

**파일**: `api/chatbot/src/main/java/com/ebson/shrimp/tm/demo/api/chatbot/service/VectorSearchServiceImpl.java`

**구현 방향**:
1. `domain/mongodb` 모듈의 `VectorSearchUtil` 활용
2. `MongoTemplate.aggregate()` 사용
3. `$vectorSearch` aggregation pipeline 실행

**구현 예시**:
```java
import com.ebson.shrimp.tm.demo.domain.mongodb.util.VectorSearchUtil;
import com.ebson.shrimp.tm.demo.domain.mongodb.util.VectorSearchOptions;
import org.bson.Document;

private List<SearchResult> searchContests(List<Float> queryVector, SearchOptions options) {
    // 1. VectorSearchOptions 생성
    VectorSearchOptions vectorOptions = VectorSearchOptions.builder()
        .indexName("vector_index_contests")
        .path("embedding_vector")
        .numCandidates(options.numCandidates())
        .limit(options.maxResults())
        .minScore(options.minSimilarityScore())
        .exact(options.exact())
        .build();
    
    // 2. aggregation pipeline 생성
    List<Document> pipeline = VectorSearchUtil.createContestSearchPipeline(
        queryVector, vectorOptions);
    
    // 3. 실행
    List<Document> results = mongoTemplate.getCollection("contests")
        .aggregate(pipeline)
        .into(new ArrayList<>());
    
    // 4. SearchResult로 변환
    return results.stream()
        .map(doc -> SearchResult.builder()
            .documentId(doc.getObjectId("_id").toString())
            .text(doc.getString("embedding_text"))
            .score(doc.getDouble("score"))
            .collectionType("CONTEST")
            .metadata(doc)
            .build())
        .collect(Collectors.toList());
}
```

**Bookmark 검색 (userId 필터 포함)**:
```java
private List<SearchResult> searchBookmarks(List<Float> queryVector, String userId, SearchOptions options) {
    // userId 필터 생성
    Document filter = new Document("user_id", userId);
    
    VectorSearchOptions vectorOptions = VectorSearchOptions.builder()
        .indexName("vector_index_bookmarks")
        .path("embedding_vector")
        .numCandidates(options.numCandidates())
        .limit(options.maxResults())
        .minScore(options.minSimilarityScore())
        .filter(filter)
        .exact(options.exact())
        .build();
    
    List<Document> pipeline = VectorSearchUtil.createBookmarkSearchPipeline(
        queryVector, userId, vectorOptions);
    
    // ... 실행 및 변환
}
```

### 3. 의존성 확인

**`api/chatbot/build.gradle`에 `domain/mongodb` 모듈 의존성 확인**:
```gradle
dependencies {
    implementation project(':domain:mongodb')
    // ...
}
```

### 4. 구현 순서

1. **`domain/mongodb` 모듈** (먼저 구현)
   - `VectorSearchIndexConfig` 클래스
   - `VectorSearchOptions` 클래스
   - `VectorSearchUtil` 클래스

2. **`api/chatbot` 모듈** (이후 구현)
   - `SearchOptions` 필드 추가
   - `VectorSearchServiceImpl` 구현

---

**작성 완료 후**: 구현된 코드가 MongoDB Atlas Vector Search 공식 문서를 완전히 준수하는지 확인하고, `api/chatbot` 모듈에서 사용 가능한지 검증합니다.
