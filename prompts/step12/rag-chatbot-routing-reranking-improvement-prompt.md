# RAG 챗봇 Intent 라우팅 및 Re-Ranking 개선 프롬프트

**작성 일시**: 2026-01-27
**대상 모듈**: `api/chatbot`

---

## 1. 개요

### 1.1 목적

`api/chatbot` 모듈의 현재 구현을 분석하여 아래 두 가지 기능의 구현 상태를 확인하고, 개선이 필요한 경우 상세 설계서를 작성한다.

1. **Intent 분류 및 라우팅**: 사용자 입력을 a) LLM 직접 요청 b) RAG 요청 c) Web 검색으로 라우팅
2. **Re-Ranking**: RAG 검색 Document들에 대해 검증된 Re-Ranking 모델 적용

### 1.2 분석 대상 파일

| 구분 | 파일 |
|------|------|
| Intent 분류 | `IntentClassificationService.java`, `IntentClassificationServiceImpl.java`, `Intent.java` |
| 라우팅 | `ChatbotServiceImpl.java` |
| Re-Ranking | `ResultRefinementChain.java` |

---

## 2. 현재 구현 분석

### 2.1 Intent 분류 및 라우팅

#### 현재 상태

**Intent.java**:
```java
public enum Intent {
    RAG_REQUIRED,           // RAG 필요
    GENERAL_CONVERSATION    // 일반 대화
}
```

**IntentClassificationServiceImpl.java**:
```java
@Override
public Intent classifyIntent(String preprocessedInput) {
    String lowerInput = preprocessedInput.toLowerCase();

    // 1. RAG 키워드 체크 (우선)
    if (containsRagKeywords(lowerInput)) {
        return Intent.RAG_REQUIRED;
    }

    // 2. 질문 형태 체크
    if (isQuestion(lowerInput)) {
        return Intent.RAG_REQUIRED;
    }

    // 3. 인사말 체크 (마지막)
    if (isGreeting(lowerInput)) {
        return Intent.GENERAL_CONVERSATION;
    }

    // 4. 기본값
    return Intent.GENERAL_CONVERSATION;
}
```

**ChatbotServiceImpl.java**:
```java
Intent intent = intentService.classifyIntent(request.message());

if (intent == Intent.GENERAL_CONVERSATION) {
    response = handleGeneralConversation(request, sessionId, chatMemory);
    sources = Collections.emptyList();
} else {
    RAGResult ragResult = handleRAGPipeline(request, sessionId, userId);
    response = ragResult.response();
    sources = ragResult.sources();
}
```

#### 분석 결과

| 라우팅 경로 | 요구사항 | 현재 구현 | 상태 |
|------------|---------|----------|------|
| a) LLM 직접 요청 | O | `GENERAL_CONVERSATION` → `handleGeneralConversation()` | 구현됨 |
| b) RAG 요청 | O | `RAG_REQUIRED` → `handleRAGPipeline()` | 구현됨 |
| c) Web 검색 | O | 미구현 | **미구현** |

**문제점**:
- `WEB_SEARCH` Intent가 정의되어 있지 않음
- Web 검색 서비스 및 라우팅 로직 없음
- 최신 정보(날씨, 뉴스, 실시간 데이터)에 대한 질문 시 RAG 또는 일반 대화로 잘못 분류됨

---

### 2.2 Re-Ranking 구현

#### 현재 상태

**ResultRefinementChain.java**:
```java
public List<SearchResult> refine(List<SearchResult> rawResults) {
    // 1. 중복 제거 (동일 문서 ID)
    List<SearchResult> deduplicated = removeDuplicates(rawResults);

    // 2. 관련성 순으로 정렬 (Vector Search score 기반)
    return deduplicated.stream()
        .sorted(Comparator.comparing(SearchResult::score).reversed())
        .collect(Collectors.toList());
}
```

#### 분석 결과

| 항목 | 요구사항 | 현재 구현 | 상태 |
|------|---------|----------|------|
| Re-Ranking 모델 | 검증된 Re-Ranking 모델 사용 | Vector Search score 기반 정렬만 사용 | **미구현** |
| Cross-Encoder | 쿼리-문서 쌍별 관련성 재평가 | 없음 | **미구현** |
| Cohere Rerank | Cohere Rerank API 활용 | 없음 | **미구현** |

**문제점**:
- Vector Search의 bi-encoder score만 사용하여 정렬
- 쿼리와 문서의 semantic 관련성을 재평가하는 Re-Ranking 단계 없음
- 검색 품질 개선을 위한 Cross-Encoder 또는 Cohere Rerank 미적용

---

## 3. 작업 지시

### 3.1 Intent 라우팅 개선

아래 내용을 포함하는 **개선 상세 설계서**를 작성하라.

#### 3.1.1 Intent Enum 확장

```java
public enum Intent {
    LLM_DIRECT,            // LLM 직접 요청 (일반 대화, 창작, 번역 등)
    RAG_REQUIRED,          // RAG 요청 (내부 데이터 검색 필요)
    WEB_SEARCH_REQUIRED    // Web 검색 요청 (최신/실시간 정보 필요)
}
```

#### 3.1.2 Intent 분류 로직 설계

다음 우선순위로 Intent를 분류하는 로직을 설계하라:

1. **WEB_SEARCH_REQUIRED 판단 기준**:
   - 최신/실시간 정보 키워드: "오늘", "현재", "최근", "지금", "today", "now", "latest", "current"
   - 외부 정보 요청: "날씨", "주가", "환율", "뉴스 속보"
   - 시간 민감성 질문: "몇 시", "언제 열리는", "마감일"

2. **RAG_REQUIRED 판단 기준** (기존 유지):
   - 내부 데이터 관련 키워드: "대회", "contest", "아카이브", "내 저장"
   - 질문 형태 + 내부 데이터 관련성

3. **LLM_DIRECT 판단 기준**:
   - 인사말, 일상 대화
   - 창작 요청: "작성해줘", "만들어줘"
   - 번역, 요약 등 텍스트 처리

#### 3.1.3 Web 검색 서비스 설계

- **WebSearchService** 인터페이스 및 구현체 설계
- 외부 검색 API 선택 (Google Custom Search API, Bing Search API, SerpAPI 등)
- 검색 결과 파싱 및 프롬프트 통합 방법
- Feign Client 구성 (`client/feign` 모듈 활용)

#### 3.1.4 ChatbotService 라우팅 로직 수정

```java
Intent intent = intentService.classifyIntent(request.message());

switch (intent) {
    case LLM_DIRECT:
        // LLM 직접 호출
        break;
    case RAG_REQUIRED:
        // RAG 파이프라인 실행
        break;
    case WEB_SEARCH_REQUIRED:
        // Web 검색 후 LLM 호출
        break;
}
```

---

### 3.2 Re-Ranking 구현

아래 내용을 포함하는 **Re-Ranking 상세 설계서**를 작성하라.

#### 3.2.1 Re-Ranking 모델 선택

다음 옵션 중 하나를 선택하여 설계하라 (공식 문서 기반):

**옵션 A: Cohere Rerank API (권장)**
- 공식 문서: https://docs.cohere.com/docs/rerank
- langchain4j 지원: `dev.langchain4j:langchain4j-cohere`
- 장점: 간단한 API 호출, 검증된 성능

**옵션 B: Cross-Encoder (sentence-transformers)**
- 모델: `cross-encoder/ms-marco-MiniLM-L-6-v2`
- HuggingFace: https://huggingface.co/cross-encoder/ms-marco-MiniLM-L-6-v2
- 장점: 무료, 로컬 실행 가능

**옵션 C: Jina Reranker**
- 공식 문서: https://jina.ai/reranker/
- langchain4j 지원: `dev.langchain4j:langchain4j-jina`

#### 3.2.2 Re-Ranking 서비스 설계

```java
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
}
```

#### 3.2.3 ResultRefinementChain 수정

```java
@Component
@RequiredArgsConstructor
public class ResultRefinementChain {

    private final ReRankingService reRankingService;

    @Value("${chatbot.reranking.enabled:true}")
    private boolean rerankingEnabled;

    @Value("${chatbot.reranking.top-k:3}")
    private int topK;

    public List<SearchResult> refine(String query, List<SearchResult> rawResults) {
        // 1. 중복 제거
        List<SearchResult> deduplicated = removeDuplicates(rawResults);

        // 2. Re-Ranking (활성화된 경우)
        if (rerankingEnabled && reRankingService != null) {
            return reRankingService.rerank(query, deduplicated, topK);
        }

        // 3. Fallback: score 기반 정렬
        return deduplicated.stream()
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }
}
```

#### 3.2.4 설정 파일 추가

```yaml
chatbot:
  reranking:
    enabled: true
    provider: cohere  # cohere, cross-encoder, jina
    top-k: 3
    model: rerank-english-v3.0  # Cohere 모델
```

---

## 4. 설계서 작성 요구사항

### 4.1 출력 파일

`docs/step12/` 디렉토리에 아래 설계서를 작성하라:

1. **rag-chatbot-intent-routing-design.md**: Intent 라우팅 개선 상세 설계서
2. **rag-chatbot-reranking-design.md**: Re-Ranking 구현 상세 설계서

### 4.2 설계서 포함 내용

각 설계서에 아래 내용을 포함하라:

1. **현재 구현 분석**: 기존 코드의 문제점 및 한계
2. **개선 목표**: 구체적인 개선 목표 명시
3. **상세 설계**:
   - 클래스/인터페이스 정의
   - 메서드 시그니처 및 동작 설명
   - 설정 파일 변경사항
4. **의존성**: 필요한 라이브러리 및 외부 API
5. **구현 순서**: 단계별 구현 계획
6. **테스트 케이스**: 주요 테스트 시나리오

### 4.3 제약사항

- **LLM 오버엔지니어링 금지**: 필요 최소한의 기능만 설계
- **불필요 추가 작업 금지**: 요청된 2가지 기능(Intent 라우팅, Re-Ranking)에 집중
- **외부 자료는 공식 출처만 참고**:
  - langchain4j: https://docs.langchain4j.dev/
  - Cohere: https://docs.cohere.com/
  - HuggingFace: https://huggingface.co/
  - Jina: https://jina.ai/

---

## 5. 참고 자료

### 5.1 현재 프로젝트 설계서

- `docs/step12/rag-chatbot-design.md`: 기존 RAG 챗봇 설계서
- `docs/step12/rag-chatbot-analysis-report.md`: 기존 분석 리포트
- `docs/step12/rag-chatbot-improvement-design.md`: 기존 개선 설계서

### 5.2 공식 문서

- langchain4j Cohere: https://docs.langchain4j.dev/integrations/scoring-reranking-models/cohere
- Cohere Rerank: https://docs.cohere.com/docs/rerank
- Cross-Encoder: https://www.sbert.net/docs/pretrained_cross-encoders.html
- Jina Reranker: https://jina.ai/reranker/
