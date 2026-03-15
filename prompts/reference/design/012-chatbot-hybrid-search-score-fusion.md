# api-chatbot 모듈 하이브리드 검색 Score Fusion 설계서 작성 프롬프트

## 역할 정의

당신은 MongoDB Atlas Vector Search와 Spring Boot 기반 RAG 시스템 전문 아키텍트입니다.
MongoDB 8.2/8.3에서 도입된 `$rankFusion`과 `$scoreFusion` 연산자의 효과를 **MongoDB 8.0 환경**에서 최대한 재현하기 위한 하이브리드 검색 개선 설계서를 작성하세요.

---

## 프로젝트 컨텍스트

### 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1, Spring Cloud 2024.0.0 |
| LLM | OpenAI GPT-4o-mini (via LangChain4j 1.0.0) |
| Embedding | OpenAI text-embedding-3-small (1536 dimensions) |
| Vector DB | MongoDB Atlas 8.0.x, Vector Search |
| Re-Ranking | Cohere rerank-multilingual-v3.0 (선택적) |
| Query Store | MongoDB Atlas, Spring Data MongoDB |
| 동기화 | Kafka 기반 CQRS |

### 현재 구현 상태

#### RAG 파이프라인 흐름 (현재)

```
사용자 입력 → IntentClassificationService (의도 분류)
  ├─ AGENT_COMMAND → AgentDelegationService
  ├─ WEB_SEARCH_REQUIRED → WebSearchService
  ├─ RAG_REQUIRED → RAG Pipeline (아래)
  └─ LLM_DIRECT → LLMService 직접 호출

RAG Pipeline:
  InputInterpretationChain (입력 해석, 최신성 키워드 감지, provider 감지)
  → VectorSearchServiceImpl (MongoDB $vectorSearch 실행)
  → ResultRefinementChain (중복 제거, Re-Ranking, Recency Boost)
  → AnswerGenerationChain (프롬프트 구성 + LLM 호출)
```

#### 현재 벡터 검색 파이프라인 (MongoDB Aggregation)

```javascript
// VectorSearchUtil.createEmergingTechSearchPipeline() 생성 파이프라인
[
  { "$vectorSearch": {
      "index": "vector_index_emerging_techs",
      "path": "embedding_vector",
      "queryVector": [/* 1536 floats */],
      "numCandidates": 100,  // recency: 150
      "limit": 10,           // recency: 15
      "filter": { "status": "PUBLISHED" }
  }},
  { "$addFields": { "score": { "$meta": "vectorSearchScore" } } },
  { "$match": { "score": { "$gte": 0.7 } } }
]
```

#### 현재 Recency Boost (Java 애플리케이션 레벨)

```java
// ResultRefinementChain.applyRecencyBoost()
// 일반 쿼리: hybridScore = similarity * 0.85 + recencyScore * 0.15
// 최신성 쿼리: hybridScore = similarity * 0.5 + recencyScore * 0.5
// recencyScore = 1.0 / (1.0 + daysSincePublished / 365.0)
```

#### 핵심 문제점

1. **최신 문서 누락**: "최신 OpenAI 업데이트 알려줘" 요청 시, 가장 최근 문서가 아닌 두 번째 최신 문서를 반환하는 현상 발생
2. **원인 분석**:
   - `$vectorSearch`는 순수 코사인 유사도만으로 상위 결과를 결정
   - 가장 최신 문서와 두 번째 최신 문서의 벡터 유사도 차이가 미세할 때, 유사도가 근소하게 높은 이전 문서가 선택됨
   - Java 레벨 Recency Boost가 적용되기 전에 이미 `$vectorSearch`의 `limit`에서 잘림
3. **근본적 한계**: 벡터 검색만으로는 "최신" 의미를 완전히 포착할 수 없음. **시간 기반 직접 쿼리와 벡터 검색을 결합하는 하이브리드 접근**이 필요

---

## 분석 대상 파일

설계서 작성 전 **반드시** 다음 파일들을 분석하세요:

### api-chatbot 모듈

| 파일 | 역할 |
|------|------|
| `api/chatbot/src/main/java/.../service/ChatbotServiceImpl.java` | RAG 파이프라인 오케스트레이션, handleRAGPipeline() |
| `api/chatbot/src/main/java/.../service/VectorSearchServiceImpl.java` | 벡터 검색 실행, searchEmergingTechs() |
| `api/chatbot/src/main/java/.../service/VectorSearchService.java` | 벡터 검색 인터페이스 |
| `api/chatbot/src/main/java/.../chain/ResultRefinementChain.java` | 결과 정제: 중복 제거, Re-Ranking, Recency Boost |
| `api/chatbot/src/main/java/.../chain/InputInterpretationChain.java` | 입력 해석, 최신성 키워드 감지, provider 감지 |
| `api/chatbot/src/main/java/.../service/dto/SearchOptions.java` | 검색 옵션 DTO (record) |
| `api/chatbot/src/main/java/.../service/dto/SearchResult.java` | 검색 결과 DTO (record) |
| `api/chatbot/src/main/java/.../service/dto/SearchContext.java` | 검색 컨텍스트 DTO |

### domain-mongodb 모듈

| 파일 | 역할 |
|------|------|
| `domain/mongodb/src/main/java/.../util/VectorSearchUtil.java` | $vectorSearch 파이프라인 빌더 (정적 팩토리) |
| `domain/mongodb/src/main/java/.../util/VectorSearchOptions.java` | 벡터 검색 옵션 (@Value @Builder) |
| `domain/mongodb/src/main/java/.../document/EmergingTechDocument.java` | Document 스키마 (published_at, embedding_vector 등) |
| `domain/mongodb/src/main/java/.../config/VectorSearchIndexConfig.java` | 벡터 인덱스 정의 |
| `domain/mongodb/src/main/java/.../repository/EmergingTechRepository.java` | Spring Data MongoDB 리포지토리 |

---

## 과제 (Task)

MongoDB 8.0 환경에서 `$rankFusion`/`$scoreFusion`의 효과를 재현하여 **벡터 검색 + 최신성 정렬**을 개선하는 **하이브리드 검색** 설계서를 작성하세요.

### 설계서에 포함해야 할 내용

#### 1. $rankFusion / $scoreFusion 분석

- 각 연산자의 공식 정의, 동작 방식, 도입 버전을 **MongoDB 공식 문서 기반**으로 정리
- 두 연산자의 차이점(위치 기반 vs 점수 기반) 비교
- RRF(Reciprocal Rank Fusion) 알고리즘 공식과 핵심 속성
- MongoDB 8.0에서 이들을 사용할 수 없는 이유와 제약

#### 2. MongoDB 8.0 하이브리드 검색 아키텍처 설계

**목표**: 벡터 검색 결과와 최신성 기반 직접 쿼리 결과를 결합하여, 의미적으로 관련성 높은 문서와 최신 문서를 모두 검색 결과에 포함

##### 2.1 하이브리드 검색 전략

두 가지 검색 소스를 병렬 실행하고 결과를 결합하는 설계:

**소스 A - 벡터 검색 (의미적 관련성)**:
```
현재의 $vectorSearch 파이프라인 유지/개선
```

**소스 B - 최신성 직접 쿼리 (시간 기반)**:
```
MongoDB 직접 쿼리: status=PUBLISHED, provider 필터, published_at DESC 정렬, limit N
```

**결합 방식 선택지** (비교 분석 필수):

| 방식 | 설명 | $rankFusion/$scoreFusion 대응 |
|------|------|------|
| **방식 A: 애플리케이션 사이드 RRF** | 각 소스의 rank 기반으로 RRF 알고리즘 적용 | `$rankFusion` 동등 |
| **방식 B: 파이프라인 내 Score Fusion** | $vectorSearch 후 $addFields로 recency decay 계산, 가중 결합 | `$scoreFusion` 동등 |
| **방식 C: 하이브리드 (A+B)** | 파이프라인 내 Score Fusion + 직접 쿼리 결과와 RRF 결합 | 두 연산자 모두 활용 |

각 방식의 장단점과 현재 문제 해결 적합성을 분석하세요.

##### 2.2 MongoDB 파이프라인 내 Score Fusion 설계

`$vectorSearch` 이후 추가 파이프라인 stage를 통해 recency score를 계산하고 결합하는 설계:

```javascript
// 설계 목표 파이프라인 구조
[
  { "$vectorSearch": { /* 기존 */ } },
  { "$addFields": { "vectorScore": { "$meta": "vectorSearchScore" } } },
  { "$addFields": {
      "recencyScore": { "$exp": { "$multiply": [-λ, /* daysSincePublished */] } }
  }},
  { "$addFields": {
      "combinedScore": { "$add": [
        { "$multiply": ["$vectorScore", vectorWeight] },
        { "$multiply": ["$recencyScore", recencyWeight] }
      ]}
  }},
  { "$sort": { "combinedScore": -1 } },
  { "$limit": maxResults }
]
```

다음 MongoDB Aggregation 연산자 활용을 검토하세요:
- `$dateDiff` (MongoDB 5.0+): 날짜 차이 계산
- `$exp`: Exponential decay 계산
- `$multiply`, `$add`: 가중 점수 결합
- `$cond`, `$ifNull`: null 안전 처리
- `$sort`, `$limit`: 최종 정렬/제한

##### 2.3 애플리케이션 사이드 결과 결합 설계

벡터 검색 결과와 직접 쿼리(최신 문서) 결과를 Java 레벨에서 결합하는 설계:

```java
// 의사코드
List<SearchResult> vectorResults = vectorSearch(query);    // 의미적 관련성 순
List<SearchResult> recencyResults = recencyQuery(provider); // 최신순

List<SearchResult> combined = fusionAlgorithm(vectorResults, recencyResults);
```

**Reciprocal Rank Fusion (RRF)** 알고리즘 구현을 포함하세요:

```
RRF_score(d) = Σ w_r * (1 / (k + rank_r(d)))
k = 60 (MongoDB 공식 기본값)
```

##### 2.4 VectorSearchOptions 확장 설계

Score Fusion을 위한 새로운 필드 추가:

| 필드 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `enableScoreFusion` | `boolean` | `false` | 파이프라인 Score Fusion 활성화 |
| `vectorWeight` | `double` | `0.85` | 벡터 유사도 가중치 |
| `recencyWeight` | `double` | `0.15` | 최신성 가중치 |
| `recencyDecayLambda` | `double` | `1.0/365.0` | Exponential decay 계수 |

##### 2.5 ResultRefinementChain 개선 설계

파이프라인 Score Fusion 적용 시 Java 레벨 Recency Boost 스킵 로직:

```java
// Score Fusion이 파이프라인에서 이미 적용된 경우
// → Java 레벨 applyRecencyBoost() 스킵
// → Re-Ranking만 적용 (필요시)
boolean scoreFusionApplied = checkScoreFusionApplied(results);
if (scoreFusionApplied) {
    // 중복 제거 + Re-Ranking만 수행
}
```

#### 3. 인터페이스 설계

- 변경/추가되는 인터페이스 전체 코드
- 구현체의 핵심 메서드 시그니처
- DTO 변경 사항

#### 4. 변경 범위 및 구현 가이드

- 수정 대상 파일 목록과 변경 내용
- 단계별 구현 순서
- 하위 호환성 보장 전략 (`enableScoreFusion=false`로 기존 동작 유지)

#### 5. 에러 처리 및 복원력

- `published_at` null 처리 (기본 recencyScore 0.5)
- 파이프라인 실행 실패 시 기존 방식 fallback
- 직접 쿼리 실패 시 벡터 검색 결과만 반환

#### 6. 테스트 전략

- 파이프라인 stage 생성 단위 테스트
- Score Fusion enabled/disabled 비교 테스트
- "최신 문서 누락" 시나리오 재현 및 해결 검증 테스트

#### 7. 성능 고려사항

- 파이프라인 내 Score Fusion vs 애플리케이션 레벨 처리 성능 비교
- `$dateDiff`, `$exp` 연산의 MongoDB 서버 부하
- 네트워크 오버헤드 감소 효과 (DB 레벨 정렬/제한)

---

## 설계 원칙

### 필수 준수

1. **객체지향 설계 기법**
   - 캡슐화: Score Fusion 설정을 VectorSearchOptions에 응집
   - 다형성: 기존 파이프라인과 Fusion 파이프라인의 인터페이스 통일
   - 합성(Composition) 선호: 기존 파이프라인 stage 재사용하여 새 파이프라인 구성

2. **SOLID 원칙**
   - SRP: VectorSearchUtil은 파이프라인 생성, VectorSearchServiceImpl은 검색 실행, ResultRefinementChain은 결과 정제
   - OCP: 새로운 Fusion 전략 추가 시 기존 코드 수정 최소화 (새 메서드 추가 방식)
   - LSP: `createEmergingTechSearchPipelineWithFusion()`은 기존 `createEmergingTechSearchPipeline()`과 동일한 반환 타입 (`List<Document>`)
   - ISP: SearchOptions에 Fusion 관련 필드를 추가하되 기존 사용자에게 영향 없는 Optional 필드
   - DIP: VectorSearchService 인터페이스 기반 의존성 주입 유지

3. **클린코드 원칙**
   - 의미 있는 명명: `createRecencyScoreStage()`, `createScoreFusionStage()`, `createFusionSortStage()`
   - 단일 책임 메서드: 각 파이프라인 stage 생성 메서드는 하나의 stage만 담당
   - DRY: VectorSearchUtil의 기존 static 팩토리 메서드 최대한 재사용
   - 작은 함수: 복잡한 파이프라인을 개별 stage 생성 메서드로 분리

4. **기존 패턴 일관성**
   - VectorSearchUtil의 정적 팩토리 메서드 패턴 유지
   - VectorSearchOptions의 @Value @Builder 패턴 유지
   - SearchOptions record 패턴 유지
   - SearchResult record를 통한 데이터 전달 유지

### 금지 사항

1. **오버엔지니어링 금지**
   - Strategy Pattern으로 Fusion 알고리즘을 추상화하지 않음 (현재 Score Fusion 하나면 충분)
   - Factory Pattern으로 파이프라인 생성을 추상화하지 않음
   - 현재 필요하지 않은 Full-Text Search ($search) 통합을 설계하지 않음

2. **불필요한 리팩토링 금지**
   - 기존 `createEmergingTechSearchPipeline()` 메서드는 수정하지 않고 새 메서드 추가
   - `ResultRefinementChain`의 기존 `applyRecencyBoost()` 로직은 보존 (fallback용)
   - 세션/메시지 관련 Service는 변경하지 않음

3. **비공식 자료 참고 금지**
   - 블로그, Stack Overflow, 비공식 튜토리얼 참고 금지
   - 모든 기술적 근거는 아래 공식 참고 자료에서만 인용

---

## 공식 참고 자료

설계서 작성 시 **반드시** 다음 공식 문서만 참고하세요:

### MongoDB 공식 문서

| 주제 | 공식 문서 URL |
|------|-------------|
| $rankFusion 연산자 | https://www.mongodb.com/docs/manual/reference/operator/aggregation/rankfusion/ |
| $scoreFusion 연산자 | https://www.mongodb.com/docs/manual/reference/operator/aggregation/scorefusion/ |
| 하이브리드 검색 가이드 | https://www.mongodb.com/docs/atlas/atlas-vector-search/hybrid-search/ |
| 벡터+풀텍스트 하이브리드 검색 | https://www.mongodb.com/docs/atlas/atlas-vector-search/hybrid-search/vector-search-with-full-text-search/ |
| RRF 하이브리드 검색 튜토리얼 | https://www.mongodb.com/docs/atlas/atlas-vector-search/tutorials/reciprocal-rank-fusion/ |
| $vectorSearch Stage | https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/ |
| Vector Search 성능 튜닝 | https://www.mongodb.com/docs/atlas/atlas-vector-search/tune-vector-search/ |
| Atlas Search Score 수정자 | https://www.mongodb.com/docs/atlas/atlas-search/score/modify-score/ |
| MongoDB Aggregation Pipeline | https://www.mongodb.com/docs/manual/reference/operator/aggregation/ |
| $dateDiff 연산자 | https://www.mongodb.com/docs/manual/reference/operator/aggregation/dateDiff/ |
| $exp 연산자 | https://www.mongodb.com/docs/manual/reference/operator/aggregation/exp/ |
| MongoDB 8.2 Release Notes | https://www.mongodb.com/docs/manual/release-notes/8.2/ |

### MongoDB 공식 블로그/리소스

| 주제 | URL |
|------|-----|
| RRF 기반 RAG 품질 향상 | https://www.mongodb.com/resources/basics/reciprocal-rank-fusion |
| $rankFusion 활용 가이드 | https://www.mongodb.com/company/blog/technical/harness-power-atlas-search-vector-search-with-rankfusion |
| $scoreFusion 네이티브 하이브리드 검색 | https://www.mongodb.com/company/blog/product-release-announcements/boost-search-relevance-mongodb-atlas-native-hybrid-search |
| $scoreFusion Public Preview 발표 | https://www.mongodb.com/products/updates/public-preview-mongodb-native-hybrid-search-with-scorefusion/ |
| 하이브리드 검색 설명 | https://www.mongodb.com/resources/products/capabilities/hybrid-search |

### 학술 논문

| 주제 | 출처 |
|------|------|
| RRF 알고리즘 원문 | Cormack, G.V., Clarke, C.L.A., Buettcher, S. (2009). "Reciprocal rank fusion outperforms condorcet and individual rank learning methods." SIGIR '09, pp. 758-759. https://dl.acm.org/doi/10.1145/1571941.1572114 |

### LangChain4j / OpenAI

| 주제 | URL |
|------|-----|
| LangChain4j RAG 가이드 | https://docs.langchain4j.dev/tutorials/rag/ |
| LangChain4j MongoDB Atlas | https://docs.langchain4j.dev/integrations/embedding-stores/mongodb-atlas/ |
| OpenAI Embeddings Guide | https://platform.openai.com/docs/guides/embeddings |

---

## 현재 코드 참조

### VectorSearchUtil.java (핵심 메서드)

```java
public final class VectorSearchUtil {
    public static final String COLLECTION_EMERGING_TECHS = "emerging_techs";
    public static final String INDEX_EMERGING_TECHS = "vector_index_emerging_techs";

    // $vectorSearch stage 생성
    public static Document createVectorSearchStage(List<Float> queryVector, VectorSearchOptions options) { ... }

    // 벡터 점수 추가 stage ($addFields + $meta)
    public static Document createScoreAddFieldsStage() {
        return new Document("$addFields",
            new Document("score", new Document("$meta", "vectorSearchScore")));
    }

    // 최소 점수 필터 stage ($match)
    public static Document createScoreFilterStage(double minScore) {
        return new Document("$match",
            new Document("score", new Document("$gte", minScore)));
    }

    // Emerging Tech 파이프라인 (현재): $vectorSearch → $addFields → $match
    public static List<Document> createEmergingTechSearchPipeline(
            List<Float> queryVector, VectorSearchOptions options) { ... }
}
```

### VectorSearchOptions.java

```java
@Value
@Builder
public class VectorSearchOptions {
    String indexName;
    String path;           // 기본값: "embedding_vector"
    int numCandidates;     // 기본값: 100
    int limit;             // 기본값: 5
    double minScore;       // 기본값: 0.7
    Document filter;
    boolean exact;         // 기본값: false

    public static class VectorSearchOptionsBuilder {
        private String path = "embedding_vector";
        private int numCandidates = 100;
        private int limit = 5;
        private double minScore = 0.7;
        private boolean exact = false;
    }
}
```

### ResultRefinementChain.java (Recency Boost 핵심 로직)

```java
// 현재 Recency Score 계산 (Hyperbolic Decay)
private double calculateRecencyScore(SearchResult result) {
    long daysSince = ChronoUnit.DAYS.between(publishedAt, LocalDateTime.now());
    return 1.0 / (1.0 + daysSince / 365.0);
}

// Hybrid Score 결합 (Java 레벨)
private List<SearchResult> applyRecencyBoost(List<SearchResult> results, boolean recencyDetected) {
    double similarityWeight = recencyDetected ? 0.5 : 0.85;
    double recencyWeight = recencyDetected ? 0.5 : 0.15;
    // hybridScore = similarity * similarityWeight + recencyScore * recencyWeight
}
```

### SearchOptions.java

```java
@Builder
public record SearchOptions(
    Boolean includeEmergingTechs,
    Integer maxResults,           // 기본값: 5
    Integer numCandidates,        // 기본값: 100
    Double minSimilarityScore,    // 기본값: 0.7
    Boolean exact,
    String providerFilter,
    Boolean recencyDetected,
    LocalDateTime dateFrom
) {}
```

### EmergingTechDocument.java (핵심 필드)

```java
@Document(collection = "emerging_techs")
public class EmergingTechDocument {
    @Id private ObjectId id;
    @Field("provider") private String provider;
    @Field("title") private String title;
    @Field("summary") private String summary;
    @Field("published_at") private LocalDateTime publishedAt;  // 최신성 기준 필드
    @Field("status") private String status;
    @Field("embedding_vector") private List<Float> embeddingVector;
    @Field("embedding_text") private String embeddingText;
}
```

### Vector Search Index 정의

```json
{
  "fields": [
    { "type": "vector", "path": "embedding_vector", "numDimensions": 1536, "similarity": "cosine" },
    { "type": "filter", "path": "provider" },
    { "type": "filter", "path": "status" },
    { "type": "filter", "path": "published_at" }
  ]
}
```

---

## 출력 형식

### 설계서 구조

```markdown
# api-chatbot 하이브리드 검색 Score Fusion 설계서

## 1. 개요
   - 배경 및 목적 (최신 문서 누락 문제)
   - 범위
   - MongoDB $rankFusion / $scoreFusion과의 관계

## 2. $rankFusion / $scoreFusion 분석
   ### 2.1 $rankFusion (위치 기반 결합)
   - 정의, 동작 방식, RRF 알고리즘 공식
   - MongoDB 버전 요구사항
   ### 2.2 $scoreFusion (점수 기반 결합)
   - 정의, 동작 방식, 점수 정규화
   - MongoDB 버전 요구사항
   ### 2.3 비교 및 MongoDB 8.0 제약

## 3. 하이브리드 검색 아키텍처 설계
   ### 3.1 검색 전략 비교 분석
   - 방식 A: 애플리케이션 사이드 RRF
   - 방식 B: 파이프라인 내 Score Fusion
   - 방식 C: 하이브리드 (A+B)
   - 선택 근거
   ### 3.2 MongoDB 파이프라인 Score Fusion 설계
   - 파이프라인 stage 상세 설계 (각 stage별 MongoDB Document)
   - Exponential Decay 함수 설계
   - 가중치 전략 (일반 쿼리 vs 최신성 쿼리)
   ### 3.3 최신성 직접 쿼리 설계
   - 쿼리 조건 및 정렬
   - 벡터 검색 결과와의 병합 전략
   ### 3.4 결과 결합 알고리즘
   - RRF 구현 상세 (k=60, 가중치)
   - 중복 제거 및 최종 정렬

## 4. 컴포넌트 설계
   ### 4.1 VectorSearchOptions 확장
   - 새 필드 정의 및 기본값
   ### 4.2 VectorSearchUtil 확장
   - 새 메서드 인터페이스 정의
   - 각 파이프라인 stage 생성 메서드
   ### 4.3 VectorSearchServiceImpl 변경
   - searchEmergingTechs() 변경 사항
   - 하이브리드 검색 로직
   ### 4.4 ResultRefinementChain 변경
   - Score Fusion 적용 시 Recency Boost 스킵 로직
   ### 4.5 SearchOptions 확장
   - 새 필드 정의

## 5. 데이터 흐름도 (Mermaid)
   - Score Fusion 활성화 시 흐름
   - Score Fusion 비활성화 시 흐름 (기존 방식)

## 6. 에러 처리 및 복원력

## 7. 성능 고려사항

## 8. 테스트 전략

## 9. 수정 파일 목록 및 변경 요약

## 10. 구현 체크리스트

## 11. 참고 자료 (공식 문서 링크만)
```

### 다이어그램 형식

- Mermaid 문법 사용
- 시퀀스 다이어그램 (검색 흐름), 클래스 다이어그램 (컴포넌트 관계) 포함

### 코드 예시 형식

- 인터페이스/DTO 변경은 전체 코드
- 파이프라인 stage는 MongoDB JSON + Java 코드 모두 표시
- 구현체는 핵심 로직만 발췌

---

## 검증 체크리스트

설계서 완성 후 다음 항목을 확인하세요:

- [ ] **최신 문서 누락 문제 해결**: "최신 OpenAI 업데이트" 요청 시 가장 최근 문서가 반드시 결과에 포함되는 메커니즘이 설계됨
- [ ] **하이브리드 접근**: 벡터 검색과 직접 쿼리(최신순)가 결합되는 설계가 포함됨
- [ ] **$rankFusion/$scoreFusion 분석**: 공식 문서 기반으로 정확히 분석됨
- [ ] **RRF 알고리즘**: Reciprocal Rank Fusion 공식과 k=60 상수가 정확히 기술됨
- [ ] **MongoDB 파이프라인 Score Fusion**: $dateDiff, $exp를 활용한 파이프라인이 설계됨
- [ ] **Exponential Decay**: `recencyScore = exp(-λ * daysSince)` 함수가 적용됨
- [ ] **가중치 전략**: 일반(85/15)과 최신성(50/50) 쿼리의 가중치가 설계됨
- [ ] **하위 호환성**: `enableScoreFusion=false`로 기존 동작을 100% 유지
- [ ] **기존 코드 수정 최소화**: 새 메서드 추가 방식, 기존 메서드 보존
- [ ] **null 안전 처리**: published_at이 null인 경우의 처리가 설계됨
- [ ] **fallback 전략**: 파이프라인 실패 시 기존 방식으로 자동 전환
- [ ] **SOLID 원칙 준수**: 특히 SRP, OCP, DIP 확인
- [ ] **오버엔지니어링 없음**: 불필요한 추상화/패턴 없음
- [ ] **모든 참고 자료가 공식 문서**: 비공식 출처 인용 없음

---

## 작업 순서

### Step 1: 현재 코드 분석 (단계적 사고)
1. 위 "분석 대상 파일" 목록의 모든 파일을 분석
2. 현재 `$vectorSearch` 파이프라인 구조 이해
3. `ResultRefinementChain`의 Recency Boost 로직 이해
4. "최신 문서 누락" 문제의 근본 원인을 파이프라인 레벨에서 분석

### Step 2: 공식 문서 리서치 (단계적 사고)
1. `$rankFusion` 공식 문서를 분석하여 RRF 알고리즘의 정확한 공식과 k=60 상수 확인
2. `$scoreFusion` 공식 문서를 분석하여 점수 정규화 방식 확인
3. MongoDB 8.0에서 사용 가능한 Aggregation 연산자 (`$dateDiff`, `$exp`) 확인
4. MongoDB 공식 하이브리드 검색 가이드에서 pre-8.2 권장 패턴 확인

### Step 3: 설계 결정 (단계적 사고)
각 설계 결정에서 **선택지 → 비교 → 결정 → 근거** 순으로 논리적으로 전개:
1. 검색 결합 방식 선택 (RRF vs Score Fusion vs 하이브리드)
2. Recency Decay 함수 선택 (Exponential vs Hyperbolic vs Gaussian)
3. 파이프라인 내 처리 vs 애플리케이션 레벨 처리 범위 결정
4. 직접 쿼리(최신순) 결과 크기 및 결합 비율 결정

### Step 4: 설계서 작성
1. 위 "출력 형식"에 따라 설계서 작성
2. 각 설계 항목에 인터페이스/메서드 시그니처 포함
3. MongoDB 파이프라인 stage를 JSON과 Java 코드 모두 표시
4. Mermaid 다이어그램 포함

### Step 5: 최종 검증
- 검증 체크리스트 모든 항목 확인
- "최신 문서 누락" 시나리오에서 설계가 문제를 해결하는지 논리적으로 검증
- 오버엔지니어링 여부 재확인
- 공식 문서 기반 내용만 포함되었는지 확인
