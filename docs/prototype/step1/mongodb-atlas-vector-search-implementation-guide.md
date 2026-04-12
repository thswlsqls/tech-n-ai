# MongoDB Atlas Vector Search 활용 설계서

**작성 일시**: 2026-01-21  
**대상**: RAG 기반 챗봇 및 시맨틱 검색 기능  
**버전**: 1.0

## 목차

1. [개요](#1-개요)
2. [현재 구현 상태 분석](#2-현재-구현-상태-분석)
3. [Vector 필드 미저장 이유](#3-vector-필드-미저장-이유)
4. [임베딩 생성 구현 설계](#4-임베딩-생성-구현-설계)
5. [Vector Search 쿼리 구현](#5-vector-search-쿼리-구현)
6. [운영 고려사항](#6-운영-고려사항)
7. [구현 체크리스트](#7-구현-체크리스트)
8. [참고 자료](#8-참고-자료)

---

## 1. 개요

### 1.1 목적

본 설계서는 현재 프로젝트에서 MongoDB Atlas Vector Search를 실질적으로 활용하기 위한 구현 가이드를 제공합니다. 특히 **Document에 Vector Search 필드가 정의되어 있으나 저장 로직이 없는 이유**를 명확히 하고, 이를 완성하기 위한 구현 방안을 제시합니다.

### 1.2 범위

| 범위 | 설명 |
|------|------|
| 포함 | 임베딩 생성 전략, Vector Search 쿼리 구현, 배치 처리 설계 |
| 제외 | Atlas 클러스터 구축 (기존 설계서 참조), LLM 통합 (별도 설계서) |

### 1.3 용어 정의

| 용어 | 정의 |
|------|------|
| **embeddingText** | 임베딩 벡터 생성 대상이 되는 텍스트 |
| **embeddingVector** | OpenAI text-embedding-3-small로 생성된 1536차원 벡터 |
| **ANN** | Approximate Nearest Neighbor, 근사 최근접 이웃 검색 |
| **$vectorSearch** | MongoDB Atlas의 벡터 유사도 검색 aggregation stage |

---

## 2. 현재 구현 상태 분석

### 2.1 Document 스키마 분석

현재 세 가지 Document에 Vector Search 필드가 정의되어 있습니다.

**ContestDocument**:
```java
@Field("embedding_text")
private String embeddingText;

@Field("embedding_vector")
private List<Float> embeddingVector;  // 1536차원
```

**NewsArticleDocument**:
```java
@Field("embedding_text")
private String embeddingText;

@Field("embedding_vector")
private List<Float> embeddingVector;  // 1536차원
```

**BookmarkDocument**:
```java
@Field("embedding_text")
private String embeddingText;

@Field("embedding_vector")
private List<Float> embeddingVector;  // 1536차원
```

### 2.2 Vector Search 유틸리티 분석

**구현 완료된 유틸리티**:

| 클래스 | 역할 | 위치 |
|--------|------|------|
| `VectorSearchUtil` | $vectorSearch pipeline 생성 | `domain/mongodb/util/` |
| `VectorSearchOptions` | 검색 옵션 정의 (limit, numCandidates 등) | `domain/mongodb/util/` |
| `VectorSearchIndexConfig` | Vector Index 정의 및 생성 가이드 | `domain/mongodb/config/` |

**VectorSearchUtil 주요 메서드**:
- `createVectorSearchStage()`: $vectorSearch stage 생성
- `createContestSearchPipeline()`: Contest 검색 pipeline
- `createNewsArticleSearchPipeline()`: NewsArticle 검색 pipeline
- `createBookmarkSearchPipeline()`: Bookmark 검색 pipeline (userId 필터 포함)

### 2.3 저장 로직 분석

**NewsServiceImpl.saveNews()**:
```java
@Transactional
@Override
public NewsArticleDocument saveNews(NewsCreateRequest request) {
    // 중복 체크
    // Document 생성 및 저장
    document.setTitle(request.title());
    document.setContent(request.content());
    // ...
    
    // ❌ embeddingText 설정 없음
    // ❌ embeddingVector 설정 없음
    
    return newsArticleRepository.save(document);
}
```

**ContestServiceImpl.saveContest()**:
```java
@Transactional
@Override
public ContestDocument saveContest(ContestCreateRequest request) {
    // 중복 체크
    // Document 생성 및 저장
    document.setTitle(request.title());
    document.setDescription(request.description());
    // ...
    
    // ❌ embeddingText 설정 없음
    // ❌ embeddingVector 설정 없음
    
    return contestRepository.save(document);
}
```

---

## 3. Vector 필드 미저장 이유

### 3.1 핵심 질문

> **Q: Document에 Vector Search용 필드가 정의되어 있으나, 저장하는 로직이 없는 이유는 무엇인가?**

### 3.2 아키텍처적 결정 배경

#### 3.2.1 관심사 분리 (Separation of Concerns)

```
┌─────────────────────────────────────────────────────────────────┐
│                        현재 아키텍처                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐    │
│  │ API Server   │     │   Batch      │     │  Embedding   │    │
│  │ (저장 담당)   │     │  (수집 담당)  │     │   Service    │    │
│  └──────┬───────┘     └──────┬───────┘     │  (임베딩 담당) │    │
│         │                    │              └──────┬───────┘    │
│         │                    │                     │            │
│         ▼                    ▼                     ▼            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    MongoDB Atlas                         │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Document                                         │    │   │
│  │  │ - title, content, ...  (저장 시점)               │    │   │
│  │  │ - embeddingText        (임베딩 서비스가 설정)     │    │   │
│  │  │ - embeddingVector      (임베딩 서비스가 설정)     │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**각 서비스의 책임**:

| 서비스 | 책임 | 임베딩 관여 |
|--------|------|------------|
| API Server | 사용자 요청 처리, 데이터 저장 | ❌ |
| Batch Source | 외부 데이터 수집/저장 | ❌ |
| Embedding Service | 임베딩 텍스트 생성, 벡터 생성, 업데이트 | ✅ |

#### 3.2.2 동기 처리의 문제점

저장 시점에 임베딩을 생성하면 발생하는 문제:

```
사용자 요청 ──▶ API Server ──▶ OpenAI API ──▶ MongoDB 저장
                               (300-500ms)
                               
                     ▲ 전체 응답 시간 증가
                     ▲ API Rate Limit 영향
                     ▲ 외부 API 장애 시 저장 실패
```

**응답 시간 비교**:

| 처리 방식 | 예상 응답 시간 | 장애 영향 |
|----------|--------------|----------|
| 동기 처리 | 500-800ms | 임베딩 API 장애 시 저장 실패 |
| 비동기 처리 | 50-100ms | 저장 성공, 임베딩은 나중에 생성 |

#### 3.2.3 비용 최적화

**OpenAI API 비용**:
- text-embedding-3-small: $0.02 / 1M tokens

**비용 최적화 전략**:

| 전략 | 설명 | 효과 |
|------|------|------|
| 배치 처리 | 여러 문서를 한 번에 임베딩 | API 호출 횟수 감소 |
| 필터링 | 이미 임베딩된 문서 제외 | 중복 비용 방지 |
| 텍스트 최적화 | content 2000자 제한 | 토큰 수 감소 |

#### 3.2.4 유연성 확보

임베딩 생성을 분리하면:

1. **모델 변경 용이**: text-embedding-3-small → 다른 모델로 교체 시 저장 로직 수정 불필요
2. **전략 변경 용이**: embeddingText 생성 규칙 변경 시 별도 처리
3. **재임베딩 가능**: 모델 변경 시 기존 문서 일괄 재처리 가능

#### 3.2.5 장애 격리

```
┌─────────────────────────────────────────────────────────┐
│                    장애 격리 효과                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  OpenAI API 장애 발생                                    │
│       │                                                 │
│       ▼                                                 │
│  ┌──────────────┐        ┌──────────────┐              │
│  │ 동기 처리     │        │ 비동기 처리   │              │
│  │              │        │              │              │
│  │ 저장 실패 ❌  │        │ 저장 성공 ✅  │              │
│  │ 사용자 오류   │        │ 임베딩만 지연  │              │
│  └──────────────┘        └──────────────┘              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 3.3 결론

**Vector 필드 미저장 이유 요약**:

| 번호 | 이유 | 설명 |
|------|------|------|
| 1 | **관심사 분리** | 데이터 저장과 임베딩 생성은 별개의 책임 |
| 2 | **성능 최적화** | 외부 API 호출을 비동기로 처리하여 응답 시간 단축 |
| 3 | **비용 효율** | 배치 처리로 OpenAI API 호출 최적화 |
| 4 | **유연성** | 임베딩 모델/전략 변경 시 저장 로직 수정 불필요 |
| 5 | **장애 격리** | 임베딩 서비스 장애가 데이터 저장에 영향 없음 |

---

## 4. 임베딩 생성 구현 설계

### 4.1 권장 전략: 배치 기반 비동기 처리

```
┌─────────────────────────────────────────────────────────────────┐
│                     권장 아키텍처                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐                                               │
│  │ API/Batch    │──(저장)──▶ MongoDB (embeddingVector = null)  │
│  └──────────────┘                                               │
│                                      │                          │
│                                      ▼                          │
│                           ┌──────────────────┐                  │
│                           │ Embedding Batch  │ (스케줄 실행)     │
│                           │                  │                  │
│                           │ 1. null 벡터 조회 │                  │
│                           │ 2. 텍스트 생성    │                  │
│                           │ 3. OpenAI 호출   │                  │
│                           │ 4. MongoDB 업데이트│                  │
│                           └────────┬─────────┘                  │
│                                    │                            │
│                                    ▼                            │
│                           MongoDB (embeddingVector = [...])     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 컴포넌트 설계

#### 4.2.1 모듈 구조

```
batch/
└── embedding/                           # 신규 모듈
    ├── src/main/java/.../batch/embedding/
    │   ├── config/
    │   │   └── EmbeddingBatchConfig.java
    │   ├── job/
    │   │   ├── ContestEmbeddingJobConfig.java
    │   │   ├── NewsArticleEmbeddingJobConfig.java
    │   │   └── BookmarkEmbeddingJobConfig.java
    │   ├── processor/
    │   │   └── EmbeddingProcessor.java
    │   ├── service/
    │   │   ├── EmbeddingTextGenerator.java
    │   │   └── EmbeddingVectorService.java
    │   └── writer/
    │       └── EmbeddingUpdateWriter.java
    └── src/main/resources/
        └── application-embedding.yml
```

#### 4.2.2 핵심 인터페이스

**EmbeddingTextGenerator**:
```java
public interface EmbeddingTextGenerator {
    
    String generateForContest(ContestDocument document);
    
    String generateForNewsArticle(NewsArticleDocument document);
    
    String generateForBookmark(BookmarkDocument document);
}
```

**EmbeddingVectorService**:
```java
public interface EmbeddingVectorService {
    
    List<Float> generateEmbedding(String text);
    
    List<List<Float>> generateEmbeddings(List<String> texts);
}
```

#### 4.2.3 임베딩 텍스트 생성 규칙

| 컬렉션 | 생성 규칙 | 최대 길이 |
|--------|----------|----------|
| contests | `title + " " + description + " " + tags.join(" ")` | 4000자 |
| news_articles | `title + " " + summary + " " + content.substring(0, 2000)` | 4000자 |
| bookmarks | `itemTitle + " " + itemSummary + " " + tag + " " + memo` | 4000자 |

**구현 예시**:
```java
@Component
public class EmbeddingTextGeneratorImpl implements EmbeddingTextGenerator {
    
    private static final int MAX_LENGTH = 4000;
    
    @Override
    public String generateForContest(ContestDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getTitle());
        
        if (doc.getDescription() != null) {
            sb.append(" ").append(doc.getDescription());
        }
        
        if (doc.getMetadata() != null && doc.getMetadata().getTags() != null) {
            sb.append(" ").append(String.join(" ", doc.getMetadata().getTags()));
        }
        
        return truncate(sb.toString(), MAX_LENGTH);
    }
    
    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
```

### 4.3 배치 Job 설계

#### 4.3.1 Job 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                    Embedding Batch Job                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Step 1: Contest Embedding                                 │  │
│  │                                                           │  │
│  │  Reader ──▶ Processor ──▶ Writer                         │  │
│  │  (null 벡터)  (임베딩 생성)  (MongoDB 업데이트)              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                          │                                      │
│                          ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Step 2: NewsArticle Embedding                             │  │
│  │                                                           │  │
│  │  Reader ──▶ Processor ──▶ Writer                         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                          │                                      │
│                          ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Step 3: Bookmark Embedding                                 │  │
│  │                                                           │  │
│  │  Reader ──▶ Processor ──▶ Writer                         │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 4.3.2 Reader 구현

```java
@Bean
public MongoItemReader<ContestDocument> contestEmbeddingReader(
        MongoTemplate mongoTemplate) {
    
    return new MongoItemReaderBuilder<ContestDocument>()
        .name("contestEmbeddingReader")
        .template(mongoTemplate)
        .targetType(ContestDocument.class)
        .jsonQuery("{ 'embedding_vector': null }")
        .sorts(Map.of("_id", Sort.Direction.ASC))
        .pageSize(100)
        .build();
}
```

#### 4.3.3 Processor 구현

```java
@Component
@RequiredArgsConstructor
public class EmbeddingProcessor<T> implements ItemProcessor<T, T> {
    
    private final EmbeddingTextGenerator textGenerator;
    private final EmbeddingVectorService vectorService;
    
    @Override
    public T process(T item) throws Exception {
        String embeddingText = generateText(item);
        List<Float> embeddingVector = vectorService.generateEmbedding(embeddingText);
        
        setEmbeddingFields(item, embeddingText, embeddingVector);
        return item;
    }
}
```

#### 4.3.4 스케줄 설정

```yaml
# application-embedding.yml
embedding:
  batch:
    cron: "0 0 */2 * * *"  # 2시간마다 실행
    chunk-size: 50
    retry-limit: 3
```

### 4.4 대안 전략

#### Option A: 이벤트 기반 비동기 처리 (실시간성 필요 시)

```
┌─────────────────────────────────────────────────────────────────┐
│                    이벤트 기반 처리                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  API Server                    Kafka                            │
│  ┌──────────┐    발행          ┌──────────┐                     │
│  │ 저장     │ ──────────────▶ │ Topic    │                     │
│  └──────────┘                  └────┬─────┘                     │
│                                     │                           │
│                                     ▼                           │
│                              ┌──────────────┐                   │
│                              │ Embedding    │                   │
│                              │ Consumer     │                   │
│                              └──────┬───────┘                   │
│                                     │                           │
│                                     ▼                           │
│                              MongoDB 업데이트                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**적용 조건**: 실시간 시맨틱 검색이 필요한 경우

#### Option B: 저장 시점 선택적 동기 처리

```java
@Transactional
public NewsArticleDocument saveNews(NewsCreateRequest request, boolean generateEmbedding) {
    NewsArticleDocument document = // ... 기존 로직
    
    if (generateEmbedding) {
        String embeddingText = textGenerator.generateForNewsArticle(document);
        List<Float> embeddingVector = vectorService.generateEmbedding(embeddingText);
        document.setEmbeddingText(embeddingText);
        document.setEmbeddingVector(embeddingVector);
    }
    
    return newsArticleRepository.save(document);
}
```

**적용 조건**: 사용자가 직접 생성한 콘텐츠 등 즉시 검색이 필요한 경우

---

## 5. Vector Search 쿼리 구현

### 5.1 기존 유틸리티 활용

**VectorSearchUtil 사용 예시**:

```java
@Service
@RequiredArgsConstructor
public class ContestVectorSearchService {
    
    private final MongoTemplate mongoTemplate;
    private final EmbeddingVectorService embeddingService;
    
    public List<ContestDocument> searchSimilarContests(String query, int limit) {
        List<Float> queryVector = embeddingService.generateEmbedding(query);
        
        VectorSearchOptions options = VectorSearchOptions.builder()
            .indexName(VectorSearchUtil.INDEX_CONTESTS)
            .limit(limit)
            .minScore(0.7)
            .filter(new Document("status", new Document("$in", List.of("UPCOMING", "ONGOING"))))
            .build();
        
        List<Document> pipeline = VectorSearchUtil.createContestSearchPipeline(queryVector, options);
        
        return mongoTemplate.aggregate(
            Aggregation.newAggregation(pipeline.stream()
                .map(doc -> context -> doc)
                .toList()),
            "contests",
            ContestDocument.class
        ).getMappedResults();
    }
}
```

### 5.2 $vectorSearch aggregation 직접 사용

```java
public List<Document> executeVectorSearch(String collectionName, List<Float> queryVector) {
    Document vectorSearchStage = new Document("$vectorSearch",
        new Document()
            .append("index", "vector_index_" + collectionName)
            .append("path", "embedding_vector")
            .append("queryVector", queryVector)
            .append("numCandidates", 100)
            .append("limit", 5)
    );
    
    Document addScoreStage = new Document("$addFields",
        new Document("score", new Document("$meta", "vectorSearchScore"))
    );
    
    Document matchScoreStage = new Document("$match",
        new Document("score", new Document("$gte", 0.7))
    );
    
    List<Document> pipeline = List.of(vectorSearchStage, addScoreStage, matchScoreStage);
    
    return mongoTemplate.getCollection(collectionName)
        .aggregate(pipeline)
        .into(new ArrayList<>());
}
```

### 5.3 검색 결과 DTO

```java
@Value
public class VectorSearchResult<T> {
    T document;
    double score;
    
    public static <T> VectorSearchResult<T> of(T document, double score) {
        return new VectorSearchResult<>(document, score);
    }
}
```

---

## 6. 운영 고려사항

### 6.1 배치 작업 스케줄

| 환경 | 스케줄 | 이유 |
|------|--------|------|
| 개발 | 수동 실행 | 테스트 용도 |
| 스테이징 | 매 4시간 | 검증 목적 |
| 프로덕션 | 매 2시간 | 실시간성과 비용 균형 |

### 6.2 모니터링 지표

| 지표 | 임계값 | 알림 |
|------|--------|------|
| 미처리 문서 수 | > 1000 | Warning |
| 배치 실패율 | > 5% | Critical |
| 평균 처리 시간 | > 10s/문서 | Warning |
| OpenAI API 오류율 | > 1% | Warning |

### 6.3 비용 예측

**월간 예상 비용 (10,000 문서 기준)**:

| 항목 | 계산 | 비용 |
|------|------|------|
| 신규 문서 임베딩 | 10,000 × 500 tokens × $0.02/1M | $0.10 |
| 쿼리 임베딩 | 50,000 쿼리 × 20 tokens × $0.02/1M | $0.02 |
| **월 예상 총액** | | **$0.12** |

### 6.4 Vector Search Index 관리

**Index 상태 확인**:
```bash
atlas clusters search indexes list \
  --clusterName tech-n-ai-cluster \
  --projectId <project-id>
```

**Index 재구축 필요 조건**:
- 대량 문서 삭제 후
- embeddingVector 차원 변경 시 (모델 변경)

---

## 7. 구현 체크리스트

### Phase 1: 기반 구현

- [ ] `batch/embedding` 모듈 생성
- [ ] `EmbeddingTextGenerator` 인터페이스 및 구현체
- [ ] `EmbeddingVectorService` 인터페이스 및 구현체 (langchain4j 사용)
- [ ] Contest Embedding Batch Job
- [ ] NewsArticle Embedding Batch Job
- [ ] Bookmark Embedding Batch Job

### Phase 2: 통합 테스트

- [ ] 로컬 환경 배치 실행 테스트
- [ ] MongoDB Atlas 연결 테스트
- [ ] Vector Search 쿼리 테스트
- [ ] 성능 테스트 (100개 문서 기준)

### Phase 3: 프로덕션 배포

- [ ] Atlas Vector Search Index 생성 확인
- [ ] 배치 스케줄 설정
- [ ] 모니터링 알림 설정
- [ ] 기존 문서 일괄 임베딩 처리

---

## 8. 참고 자료

### 공식 문서

- [MongoDB Atlas Vector Search](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [$vectorSearch aggregation](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/)
- [Vector Search Index 생성](https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/)
- [OpenAI Embeddings](https://platform.openai.com/docs/guides/embeddings)
- [langchain4j](https://docs.langchain4j.dev/)

### 프로젝트 내 관련 설계서

- `docs/step1/2. mongodb-schema-design.md`: Document 스키마 설계
- `docs/step1/6. mongodb-atlas-integration-guide.md`: Atlas 연동 가이드
- `docs/step12/rag-chatbot-design.md`: RAG 챗봇 설계

### 프로젝트 내 관련 코드

- `domain/mongodb/util/VectorSearchUtil.java`
- `domain/mongodb/util/VectorSearchOptions.java`
- `domain/mongodb/config/VectorSearchIndexConfig.java`

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-21  
**작성자**: Vector Search Architect
