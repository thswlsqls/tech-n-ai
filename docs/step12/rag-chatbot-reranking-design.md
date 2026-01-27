# RAG 챗봇 Re-Ranking 구현 상세 설계서

**작성 일시**: 2026-01-27
**대상 모듈**: `api/chatbot`
**참고 문서**:
- `docs/step12/rag-chatbot-analysis-report.md`
- `docs/step12/rag-chatbot-improvement-design.md`
- `prompts/step12/rag-chatbot-routing-reranking-improvement-prompt.md`

---

## 1. 현재 구현 분석

### 1.1 현재 ResultRefinementChain

**파일**: `api/chatbot/.../chain/ResultRefinementChain.java`

```java
@Slf4j
@Component
public class ResultRefinementChain {

    public List<SearchResult> refine(List<SearchResult> rawResults) {
        // 1. 중복 제거 (동일 문서 ID)
        List<SearchResult> deduplicated = removeDuplicates(rawResults);

        // 2. 관련성 순으로 정렬 (Vector Search score 기반)
        return deduplicated.stream()
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .collect(Collectors.toList());
    }

    private List<SearchResult> removeDuplicates(List<SearchResult> results) {
        Set<String> seenIds = new HashSet<>();
        return results.stream()
            .filter(r -> seenIds.add(r.documentId()))
            .collect(Collectors.toList());
    }
}
```

### 1.2 문제점

| 항목 | 문제점 | 영향 |
|------|--------|------|
| 정렬 방식 | Vector Search score(bi-encoder)만 사용 | 쿼리-문서 의미적 관련성 정밀 평가 불가 |
| Re-Ranking 모델 | 전용 Re-Ranking 모델 미사용 | 검색 품질 최적화 한계 |
| Cross-Encoder | 쿼리-문서 쌍별 평가 없음 | 상위 결과의 정확도 개선 불가 |

### 1.3 Re-Ranking의 필요성

RAG 시스템에서 검색 품질은 최종 답변 품질에 직접적인 영향을 미친다.

1. **Bi-Encoder vs Cross-Encoder**:
   - Bi-Encoder (Vector Search): 쿼리와 문서를 독립적으로 임베딩 → 빠르지만 정밀도 낮음
   - Cross-Encoder (Re-Ranking): 쿼리-문서 쌍을 함께 처리 → 느리지만 정밀도 높음

2. **Two-Stage Retrieval**:
   - 1단계: Vector Search로 후보 문서 빠르게 검색 (recall 최적화)
   - 2단계: Re-Ranking으로 상위 문서 재정렬 (precision 최적화)

---

## 2. 개선 목표

1. **Re-Ranking 서비스 설계**: 검증된 Re-Ranking 모델 연동
2. **ResultRefinementChain 수정**: Re-Ranking 서비스 통합
3. **설정 기반 활성화/비활성화**: 환경별 유연한 적용

---

## 3. Re-Ranking 모델 선택

### 3.1 옵션 비교

| 옵션 | 모델 | 장점 | 단점 | 권장 |
|------|------|------|------|------|
| **A: Cohere Rerank** | rerank-multilingual-v3.0 | API 호출만으로 사용 가능, 검증된 성능, 다국어 지원 | 유료 API, 외부 의존성 | **권장** |
| B: Jina Reranker | jina-reranker-v2-base-multilingual | 100+ 언어 지원, 무료 티어 제공 | API 호출 필요 | 대안 |
| C: Cross-Encoder (로컬) | ms-marco-MiniLM-L-6-v2 | 무료, 로컬 실행 | 인프라 필요, 영어 최적화 | 비권장 |

### 3.2 권장 선택: Cohere Rerank

**선택 이유**:
1. langchain4j 공식 지원 (`dev.langchain4j:langchain4j-cohere`)
2. 다국어 지원 (`rerank-multilingual-v3.0`)
3. 검증된 성능 (RAG 벤치마크에서 상위권)
4. 간단한 API 호출로 통합 가능

**공식 문서**:
- langchain4j Cohere: https://docs.langchain4j.dev/integrations/scoring-reranking-models/cohere
- Cohere Rerank: https://docs.cohere.com/reference/rerank

---

## 4. 상세 설계

### 4.1 의존성 추가

**파일**: `api/chatbot/build.gradle`

```gradle
dependencies {
    // 기존 의존성 유지...

    // langchain4j Cohere (Re-Ranking)
    implementation 'dev.langchain4j:langchain4j-cohere:0.35.0'
}
```

### 4.2 ReRankingService 인터페이스

**파일**: `api/chatbot/.../service/ReRankingService.java`

```java
package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import java.util.List;

/**
 * Re-Ranking 서비스 인터페이스
 */
public interface ReRankingService {

    /**
     * 검색 결과를 쿼리와의 관련성 기준으로 재정렬
     *
     * @param query 사용자 쿼리
     * @param documents 검색된 문서 목록
     * @param topK 반환할 상위 문서 수
     * @return 재정렬된 문서 목록
     */
    List<SearchResult> rerank(String query, List<SearchResult> documents, int topK);

    /**
     * Re-Ranking 서비스 활성화 여부
     *
     * @return 활성화 여부
     */
    boolean isEnabled();
}
```

### 4.3 CohereReRankingServiceImpl 구현체

**파일**: `api/chatbot/.../service/CohereReRankingServiceImpl.java`

```java
package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

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
                log.info("Cohere Re-Ranking service initialized with model: {}", modelName);
            } catch (Exception e) {
                log.error("Failed to initialize Cohere Re-Ranking service", e);
                this.scoringModel = null;
            }
        } else {
            log.info("Cohere Re-Ranking service is disabled");
        }
    }

    @Override
    public List<SearchResult> rerank(String query, List<SearchResult> documents, int topK) {
        if (!isEnabled() || documents.isEmpty()) {
            log.debug("Re-Ranking skipped: enabled={}, documents={}", isEnabled(), documents.size());
            return documents.stream()
                .sorted(Comparator.comparing(SearchResult::score).reversed())
                .limit(topK)
                .toList();
        }

        try {
            // 1. SearchResult를 TextSegment로 변환
            List<TextSegment> segments = documents.stream()
                .map(doc -> TextSegment.from(doc.text()))
                .toList();

            // 2. Cohere Scoring Model로 점수 계산
            Response<List<Double>> response = scoringModel.scoreAll(segments, query);
            List<Double> scores = response.content();

            // 3. 점수와 원본 문서 매핑 후 정렬
            List<SearchResult> rerankedResults = IntStream.range(0, documents.size())
                .mapToObj(i -> {
                    SearchResult original = documents.get(i);
                    double newScore = scores.get(i);
                    return SearchResult.builder()
                        .documentId(original.documentId())
                        .text(original.text())
                        .score(newScore)  // Re-Ranking 점수로 교체
                        .collectionType(original.collectionType())
                        .metadata(original.metadata())
                        .build();
                })
                .filter(doc -> doc.score() >= minScore)  // 최소 점수 필터링
                .sorted(Comparator.comparing(SearchResult::score).reversed())
                .limit(topK)
                .toList();

            log.debug("Re-Ranking completed: {} documents -> {} results", documents.size(), rerankedResults.size());
            return rerankedResults;

        } catch (Exception e) {
            log.error("Re-Ranking failed, falling back to original order", e);
            return documents.stream()
                .sorted(Comparator.comparing(SearchResult::score).reversed())
                .limit(topK)
                .toList();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && scoringModel != null;
    }
}
```

### 4.4 ResultRefinementChain 수정

**파일**: `api/chatbot/.../chain/ResultRefinementChain.java`

```java
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

    @Value("${chatbot.reranking.top-k:3}")
    private int topK;

    /**
     * 검색 결과 정제 및 Re-Ranking
     *
     * @param query 사용자 쿼리 (Re-Ranking에 필요)
     * @param rawResults 원본 검색 결과
     * @return 정제 및 재정렬된 검색 결과
     */
    public List<SearchResult> refine(String query, List<SearchResult> rawResults) {
        // 1. 중복 제거 (동일 문서 ID)
        List<SearchResult> deduplicated = removeDuplicates(rawResults);

        if (deduplicated.isEmpty()) {
            return deduplicated;
        }

        // 2. Re-Ranking (활성화된 경우)
        if (reRankingService.isEnabled()) {
            log.debug("Applying Re-Ranking to {} documents", deduplicated.size());
            return reRankingService.rerank(query, deduplicated, topK);
        }

        // 3. Fallback: Vector Search score 기반 정렬
        log.debug("Re-Ranking disabled, using vector search score");
        return deduplicated.stream()
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * 중복 제거 (기존 메서드 유지)
     */
    private List<SearchResult> removeDuplicates(List<SearchResult> results) {
        Set<String> seenIds = new HashSet<>();
        return results.stream()
            .filter(r -> seenIds.add(r.documentId()))
            .collect(Collectors.toList());
    }
}
```

### 4.5 ChatbotServiceImpl 수정

**파일**: `api/chatbot/.../service/ChatbotServiceImpl.java`

```java
private RAGResult handleRAGPipeline(ChatRequest request, String sessionId, Long userId) {
    SearchQuery searchQuery = inputChain.interpret(request.message());
    SearchOptions searchOptions = buildSearchOptions(searchQuery);

    List<SearchResult> searchResults =
        vectorSearchService.search(searchQuery.query(), userId, searchOptions);

    // refine() 메서드에 query 파라미터 추가
    List<SearchResult> refinedResults = refinementChain.refine(
        request.message(),  // 쿼리 추가
        searchResults
    );

    String response = answerChain.generate(request.message(), refinedResults);

    List<SourceResponse> sources = refinedResults.stream()
        .map(r -> SourceResponse.builder()
            .documentId(r.documentId())
            .collectionType(r.collectionType())
            .score(r.score())
            .build())
        .collect(Collectors.toList());

    return new RAGResult(response, sources);
}
```

---

## 5. 설정 파일 변경사항

### 5.1 application-chatbot.yml

```yaml
chatbot:
  # 기존 설정 유지...

  # Re-Ranking 설정 추가
  reranking:
    enabled: false  # 기본값: 비활성화
    api-key: ${COHERE_API_KEY:}
    model-name: rerank-multilingual-v3.0
    top-k: 3
    min-score: 0.3
```

### 5.2 환경별 설정 예시

**application-local.yml** (개발 환경):
```yaml
chatbot:
  reranking:
    enabled: false  # 개발 시 비활성화 (비용 절감)
```

**application-prod.yml** (운영 환경):
```yaml
chatbot:
  reranking:
    enabled: true
    api-key: ${COHERE_API_KEY}
    model-name: rerank-multilingual-v3.0
    top-k: 3
    min-score: 0.3
```

---

## 6. 의존성

### 6.1 Gradle 의존성

```gradle
// api/chatbot/build.gradle
dependencies {
    // langchain4j Cohere (Re-Ranking)
    implementation 'dev.langchain4j:langchain4j-cohere:0.35.0'
}
```

### 6.2 외부 API

| API | 용도 | 공식 문서 |
|-----|------|----------|
| Cohere Rerank API | 문서 재정렬 | https://docs.cohere.com/reference/rerank |

### 6.3 Cohere API Key 발급

1. https://dashboard.cohere.com/ 접속
2. 계정 생성 또는 로그인
3. API Keys 메뉴에서 키 생성
4. 환경 변수 `COHERE_API_KEY`로 설정

---

## 7. 구현 순서

| 순서 | 항목 | 수정 파일 | 복잡도 |
|------|------|----------|--------|
| 1 | 의존성 추가 | `build.gradle` | Low |
| 2 | ReRankingService 인터페이스 생성 | 신규 파일 | Low |
| 3 | CohereReRankingServiceImpl 구현 | 신규 파일 | Medium |
| 4 | ResultRefinementChain 수정 | `ResultRefinementChain.java` | Low |
| 5 | ChatbotServiceImpl 수정 | `ChatbotServiceImpl.java` | Low |
| 6 | 설정 파일 수정 | `application-chatbot.yml` | Low |
| 7 | 테스트 작성 | 신규 테스트 파일 | Medium |

---

## 8. 테스트 케이스

### 8.1 ReRankingService 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class CohereReRankingServiceImplTest {

    @InjectMocks
    private CohereReRankingServiceImpl reRankingService;

    @Test
    void rerank_whenDisabled_shouldReturnOriginalOrder() {
        // given
        ReflectionTestUtils.setField(reRankingService, "enabled", false);
        String query = "테스트 쿼리";
        List<SearchResult> documents = createTestDocuments();

        // when
        List<SearchResult> result = reRankingService.rerank(query, documents, 3);

        // then
        assertThat(result).hasSize(3);
        // score 내림차순 정렬 확인
    }

    @Test
    void rerank_whenEmptyDocuments_shouldReturnEmptyList() {
        // given
        String query = "테스트 쿼리";
        List<SearchResult> documents = Collections.emptyList();

        // when
        List<SearchResult> result = reRankingService.rerank(query, documents, 3);

        // then
        assertThat(result).isEmpty();
    }

    private List<SearchResult> createTestDocuments() {
        return List.of(
            SearchResult.builder()
                .documentId("1")
                .text("첫 번째 문서")
                .score(0.8)
                .collectionType("contests")
                .build(),
            SearchResult.builder()
                .documentId("2")
                .text("두 번째 문서")
                .score(0.7)
                .collectionType("contests")
                .build(),
            SearchResult.builder()
                .documentId("3")
                .text("세 번째 문서")
                .score(0.6)
                .collectionType("news")
                .build()
        );
    }
}
```

### 8.2 ResultRefinementChain 통합 테스트

```java
@SpringBootTest
class ResultRefinementChainTest {

    @Autowired
    private ResultRefinementChain refinementChain;

    @MockBean
    private ReRankingService reRankingService;

    @Test
    void refine_withReRankingEnabled_shouldCallReRankingService() {
        // given
        String query = "Kaggle 대회 정보";
        List<SearchResult> rawResults = createTestDocuments();

        when(reRankingService.isEnabled()).thenReturn(true);
        when(reRankingService.rerank(eq(query), anyList(), anyInt()))
            .thenReturn(rawResults.subList(0, 2));

        // when
        List<SearchResult> result = refinementChain.refine(query, rawResults);

        // then
        verify(reRankingService).rerank(eq(query), anyList(), anyInt());
        assertThat(result).hasSize(2);
    }

    @Test
    void refine_withReRankingDisabled_shouldUseVectorSearchScore() {
        // given
        String query = "Kaggle 대회 정보";
        List<SearchResult> rawResults = createTestDocuments();

        when(reRankingService.isEnabled()).thenReturn(false);

        // when
        List<SearchResult> result = refinementChain.refine(query, rawResults);

        // then
        verify(reRankingService, never()).rerank(anyString(), anyList(), anyInt());
        // score 내림차순 정렬 확인
    }
}
```

### 8.3 E2E 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class ChatbotReRankingE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chat_withRagIntent_shouldReturnReRankedSources() throws Exception {
        // given
        String requestBody = """
            {
                "message": "최근 Kaggle 대회 알려줘"
            }
            """;

        // when & then
        mockMvc.perform(post("/api/chatbot/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").isNotEmpty())
            .andExpect(jsonPath("$.sources").isArray());
    }
}
```

---

## 9. 모니터링 및 운영

### 9.1 로깅

```java
// Re-Ranking 전후 비교 로깅
log.info("Re-Ranking applied: original_count={}, reranked_count={}, top_score={}",
    originalCount, rerankedResults.size(),
    rerankedResults.isEmpty() ? "N/A" : rerankedResults.get(0).score());
```

### 9.2 메트릭

| 메트릭 | 설명 |
|--------|------|
| `chatbot.reranking.request.count` | Re-Ranking 요청 수 |
| `chatbot.reranking.request.duration` | Re-Ranking 처리 시간 |
| `chatbot.reranking.error.count` | Re-Ranking 실패 수 |

### 9.3 비용 고려사항

| 항목 | 내용 |
|------|------|
| Cohere Rerank 가격 | 문서 1,000개당 약 $0.001 (모델에 따라 다름) |
| 비용 최적화 | `topK` 값 조정으로 Re-Ranking 대상 문서 수 제한 |
| Fallback | API 실패 시 Vector Search score로 자동 fallback |

---

## 10. 참고 자료

### 10.1 프로젝트 내부 문서

- `docs/step12/rag-chatbot-design.md`: 기존 RAG 챗봇 설계서
- `docs/step12/rag-chatbot-analysis-report.md`: 기존 분석 리포트

### 10.2 공식 문서

- langchain4j Cohere: https://docs.langchain4j.dev/integrations/scoring-reranking-models/cohere
- langchain4j Examples (Re-Ranking): https://github.com/langchain4j/langchain4j-examples/blob/main/rag-examples/src/main/java/_3_advanced/_03_Advanced_RAG_with_ReRanking_Example.java
- Cohere Rerank API: https://docs.cohere.com/reference/rerank
- Cohere Dashboard: https://dashboard.cohere.com/

### 10.3 대안 옵션

- Jina Reranker: https://jina.ai/reranker (무료 티어 10M 토큰)
- langchain4j Jina: https://docs.langchain4j.dev/integrations/scoring-reranking-models/jina
