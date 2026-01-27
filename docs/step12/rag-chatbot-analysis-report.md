# RAG 기반 멀티턴 채팅 기능 구현 분석 리포트

**분석 일시**: 2026-01-26
**분석 대상 모듈**: `api/chatbot`
**참고 설계서**: `docs/step12/rag-chatbot-design.md`

---

## 1. 분석 대상 파일 목록

| 구분 | 파일 |
|------|------|
| 입력 전처리 | `InputPreprocessingServiceImpl.java` |
| 인텐트 분류 | `IntentClassificationServiceImpl.java` |
| Vector Search | `VectorSearchServiceImpl.java`, `VectorSearchUtil.java`, `VectorSearchOptions.java` |
| 토큰 제어 | `TokenServiceImpl.java`, `LangChain4jConfig.java` |
| 프롬프트 체인 | `InputInterpretationChain.java`, `ResultRefinementChain.java`, `AnswerGenerationChain.java`, `PromptServiceImpl.java`, `ChatbotServiceImpl.java` |
| LLM | `LLMServiceImpl.java` |

---

## 2. 분석 결과 요약

| 항목 | 현재 상태 | 개선 필요 여부 | 우선순위 |
|------|----------|--------------|---------|
| 입력 전처리 | 기본 구현 완료 | Prompt Injection 방지 필요 | Medium |
| 인텐트 분류 | 키워드 기반 분류 구현 | 우선순위 로직 오류 | High |
| Vector Search | MongoDB $vectorSearch 구현 완료 | 구현 적절함 | Low |
| 토큰 제어 | 휴리스틱 추정만 사용 | OpenAiTokenCountEstimator 미적용 | High |
| 프롬프트 체인 | 체인 구조 구현 완료 | 불필요한 중복/변환 존재 | Medium |

---

## 3. 상세 분석

### 3.1 사용자 입력 전처리

#### 현재 구현 (`InputPreprocessingServiceImpl.java`)

```java
// 검증 로직
if (rawInput == null || rawInput.isBlank()) { throw InvalidInputException }
if (rawInput.length() > maxLength) { throw InvalidInputException }
if (rawInput.length() < minLength) { throw InvalidInputException }

// 정규화
String trimmed = input.trim();
trimmed = trimmed.replaceAll("\\s+", " ");

// 특수문자 필터링
String cleaned = input.replaceAll("[\\x00-\\x1F\\x7F]", " ");
```

#### 설계서 대비 준수 여부

| 항목 | 설계서 | 구현 | 일치 |
|------|--------|------|------|
| max-length | 500 | `@Value` 기본값 500 | O |
| min-length | 1 | `@Value` 기본값 1 | O |
| 제어문자 제거 | `[\\x00-\\x1F\\x7F]` | 동일 | O |
| 연속 공백 변환 | `\\s+` → ` ` | 동일 | O |

#### 개선점

**1. Prompt Injection 방지 미구현**
- 현재 입력에서 악의적인 프롬프트 주입 패턴을 필터링하지 않음
- 예: `"Ignore previous instructions and..."`, `"<|system|>"` 등

**2. HTML 엔티티 미처리**
- `&lt;`, `&gt;`, `&amp;` 등 HTML 엔티티가 그대로 전달될 수 있음

#### 권장 조치

```java
// Prompt Injection 패턴 필터링 추가 (선택적)
private static final Pattern INJECTION_PATTERN = Pattern.compile(
    "(?i)(ignore (previous|all) instructions|<\\|[a-z]+\\|>|\\[INST\\]|\\[/INST\\])"
);

private String sanitizeForInjection(String input) {
    if (INJECTION_PATTERN.matcher(input).find()) {
        log.warn("Potential prompt injection detected: {}", input);
        // 정책에 따라 필터링 또는 거부
    }
    return input;
}
```

**우선순위**: Medium (운영 환경 보안 요구사항에 따라 결정)

---

### 3.2 인텐트 구분 로직

#### 현재 구현 (`IntentClassificationServiceImpl.java`)

```java
public Intent classifyIntent(String preprocessedInput) {
    String lowerInput = preprocessedInput.toLowerCase();

    // 1. 인사말 체크 (우선)
    if (isGreeting(lowerInput)) {
        return Intent.GENERAL_CONVERSATION;
    }

    // 2. RAG 키워드 체크
    if (containsRagKeywords(lowerInput)) {
        return Intent.RAG_REQUIRED;
    }

    // 3. 질문 형태 체크
    if (isQuestion(lowerInput)) {
        return Intent.RAG_REQUIRED;
    }

    // 4. 기본값
    return Intent.GENERAL_CONVERSATION;
}
```

#### 문제점 분석

**1. 인사말 우선순위 오류 (Critical)**

```
입력: "안녕하세요 대회 정보 알려줘"
현재 결과: GENERAL_CONVERSATION (인사말 "안녕"이 먼저 매칭)
기대 결과: RAG_REQUIRED (대회 정보 검색 필요)
```

인사말이 포함되면 RAG 키워드가 있어도 일반 대화로 분류됨.

**2. 키워드 세트 하드코딩**

```java
private static final Set<String> GREETING_KEYWORDS = Set.of(
    "안녕", "안녕하세요", "하이", "hi", "hello", "헬로"
);
```

확장성 부족 - 새 키워드 추가 시 코드 수정 필요.

**3. 의문사 패턴 불완전**

```java
boolean hasQuestionWords = input.matches(".*(무엇|어떤|어디|언제|누가|왜|어떻게).*");
```

"뭐", "몇", "얼마" 등 구어체 의문사 미포함.

#### 권장 조치

```java
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
    if (isGreeting(lowerInput) && !containsRagKeywords(lowerInput)) {
        return Intent.GENERAL_CONVERSATION;
    }

    return Intent.GENERAL_CONVERSATION;
}
```

**우선순위**: High (사용자 의도 오분류 직접 영향)

---

### 3.3 MongoDB Atlas Vector Search 구현

#### 현재 구현 (`VectorSearchServiceImpl.java`, `VectorSearchUtil.java`)

```java
// 1. 쿼리 임베딩 생성
Embedding embedding = embeddingModel.embed(query).content();
List<Float> queryVector = embedding.vectorAsList();

// 2. $vectorSearch aggregation pipeline 생성
Document vectorSearchParams = new Document()
    .append("index", indexName)
    .append("path", path)                    // "embedding_vector"
    .append("queryVector", queryVector)
    .append("limit", limit)
    .append("numCandidates", numCandidates); // 100

// 3. score 추출
.append("score", new Document("$meta", "vectorSearchScore"))

// 4. minScore 필터링
new Document("$match", new Document("score", new Document("$gte", minScore)))
```

#### MongoDB 공식 문서 대비 검증

| 파라미터 | 공식 문서 | 구현 | 일치 |
|---------|----------|------|------|
| index | 필수 | O | O |
| path | 필수 | "embedding_vector" | O |
| queryVector | 필수 | List<Float> | O |
| numCandidates | ANN 필수 | 100 (limit의 20배) | O |
| limit | 필수 | 5 | O |
| filter | 선택 (pre-filter) | userId 필터 구현 | O |
| exact | 선택 | false (ANN) | O |

#### 구현 상태: 적절함

**확인된 정상 구현:**
- OpenAI text-embedding-3-small 사용 (1536 dimensions)
- document/query 구분 없이 동일 모델 사용 (OpenAI 특성 반영)
- pre-filter로 archives의 userId 필터링 적용
- `$meta: "vectorSearchScore"`로 유사도 점수 추출
- numCandidates: 100 (limit 5의 20배, 권장 범위 내)

#### 잠재적 주의사항

```java
// VectorSearchOptions 빌더 기본값
private String path = "embedding_vector";
```

MongoDB Atlas Vector Index 정의 시 `path` 필드가 `"embedding_vector"`로 일치해야 함.
운영 환경에서 Index 정의 확인 필요.

**우선순위**: Low (현재 구현 적절)

---

### 3.4 토큰 사용량 제어

#### 현재 구현 (`TokenServiceImpl.java`)

```java
public int estimateTokens(String text) {
    int wordCount = text.split("\\s+").length;
    int koreanCharCount = (int) text.chars()
        .filter(c -> c >= 0xAC00 && c <= 0xD7A3)
        .count();

    // 한국어 문자는 약 2 토큰, 영어 단어는 약 1.3 토큰
    int estimatedTokens = (int) (koreanCharCount * 2 + (wordCount - koreanCharCount) * 1.3);

    return Math.max(estimatedTokens, text.length() / 4);
}
```

#### 문제점 분석

**1. OpenAiTokenCountEstimator 미적용 (Critical)**

설계서 (`LangChain4jConfig.java` 설계):
```java
@Bean
public TokenCountEstimator tokenCountEstimator() {
    return new OpenAiTokenCountEstimator(chatModelName);
}
```

현재 구현 (`LangChain4jConfig.java:84-89`):
```java
// @Bean
// public TokenCountEstimator tokenCountEstimator() {
//     // TODO: ChatMemory 구현 시점에 TokenCountEstimator Bean 추가 필요
//     // return new OpenAiTokenCountEstimator(chatModelName);
// }
```

**주석 처리되어 Bean 미생성.**

**2. 휴리스틱 추정의 부정확성**

| 텍스트 예시 | 휴리스틱 추정 | 실제 토큰 (tiktoken) | 오차 |
|------------|--------------|---------------------|------|
| "안녕하세요" | ~10 | 5 | +100% |
| "Hello world" | ~3 | 2 | +50% |
| 혼합 텍스트 500자 | ~200 | 150-180 | +10~30% |

**3. 실제 LLM 응답 토큰 수 미수집**

`LLMServiceImpl.java`:
```java
public String generate(String prompt) {
    return chatLanguageModel.generate(prompt);  // String만 반환
}
```

langchain4j의 `ChatLanguageModel.generate()`는 `Response<AiMessage>`를 반환하여 `tokenUsage()` 접근 가능하나, 현재 String만 반환.

#### 권장 조치

**Step 1: OpenAiTokenCountEstimator Bean 활성화**
```java
@Bean
public TokenCountEstimator tokenCountEstimator() {
    return new OpenAiTokenCountEstimator(chatModelName);
}
```

**Step 2: TokenService에서 TokenCountEstimator 사용**
```java
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final TokenCountEstimator tokenCountEstimator;

    @Override
    public int estimateTokens(String text) {
        return tokenCountEstimator.estimateTokenCount(text);
    }
}
```

**Step 3: LLMService에서 실제 토큰 수 반환 (선택적)**
```java
public record LLMResponse(String content, int inputTokens, int outputTokens) {}

public LLMResponse generate(String prompt) {
    Response<AiMessage> response = chatLanguageModel.generate(prompt);
    TokenUsage usage = response.tokenUsage();
    return new LLMResponse(
        response.content().text(),
        usage.inputTokenCount(),
        usage.outputTokenCount()
    );
}
```

**우선순위**: High (비용 추적 정확도 직접 영향)

---

### 3.5 프롬프트 체인 구현

#### 현재 구현 분석

**InputInterpretationChain.java**
```java
private String extractSearchQuery(String cleanedInput) {
    return input;  // 단순 반환, 실제 추출 로직 없음
}
```

`extractSearchQuery`가 입력을 그대로 반환. 노이즈 제거 외 추가 처리 없음.

**ResultRefinementChain.java**
```java
List<SearchResult> filtered = rawResults.stream()
    .filter(r -> r.score() >= minSimilarityScore)  // 중복 필터링
    .collect(Collectors.toList());
```

`VectorSearchServiceImpl`에서 이미 `minSimilarityScore` 필터링 수행:
```java
// VectorSearchUtil.java:171-173
if (options.getMinScore() > 0) {
    pipeline.add(createScoreFilterStage(options.getMinScore()));
}
```

**AnswerGenerationChain.java**
```java
// RefinedResult → SearchResult 불필요한 역변환
List<SearchResult> searchResults = refinedResults.stream()
    .map(r -> SearchResult.builder()
        .documentId(r.documentId())
        .text(r.text())
        ...
        .build())
    .toList();

String prompt = promptService.buildPrompt(query, searchResults);
```

`RefinedResult`와 `SearchResult`가 동일한 필드 구조 → 변환 불필요.

**ChatbotServiceImpl.java - 멀티턴 히스토리 중복 로드**
```java
private void saveMessagesToMemory(String sessionId, String userMessage, String assistantMessage) {
    ChatMemory chatMemory = memoryProvider.get(sessionId);
    List<ChatMessage> history = messageService.getMessagesForMemory(sessionId, null);
    history.forEach(chatMemory::add);  // 히스토리 전체 재로드

    chatMemory.add(UserMessage.from(userMessage));    // 현재 메시지 추가
    chatMemory.add(AiMessage.from(assistantMessage));
}
```

매 호출마다 전체 히스토리를 다시 로드하고 ChatMemory에 추가 → 메모리 중복 가능성.

#### 체인 구조 개선점

| 체인 | 문제점 | 영향도 |
|------|--------|--------|
| InputInterpretationChain | extractSearchQuery 미구현 | Low |
| ResultRefinementChain | 유사도 필터링 중복 | Low |
| AnswerGenerationChain | RefinedResult→SearchResult 불필요 변환 | Low |
| ChatbotServiceImpl | 히스토리 중복 로드 | Medium |

#### 권장 조치

**1. ResultRefinementChain 중복 필터링 제거**
```java
public List<RefinedResult> refine(List<SearchResult> rawResults) {
    // minSimilarityScore 필터링은 VectorSearchService에서 이미 수행
    // 여기서는 중복 제거와 정렬만 수행
    List<SearchResult> deduplicated = removeDuplicates(rawResults);
    ...
}
```

**2. PromptService가 RefinedResult 직접 사용**
```java
public interface PromptService {
    String buildPrompt(String query, List<RefinedResult> refinedResults);
}
```

**3. 히스토리 로드 로직 최적화**
```java
private void saveMessagesToMemory(String sessionId, String userMessage, String assistantMessage) {
    // ChatMemory는 이미 이전 메시지를 포함하고 있으므로 재로드 불필요
    ChatMemory chatMemory = memoryProvider.get(sessionId);

    // DB에만 저장
    messageService.saveMessage(sessionId, "USER", userMessage, ...);
    messageService.saveMessage(sessionId, "ASSISTANT", assistantMessage, ...);
}
```

**우선순위**: Medium (기능 정상 작동, 코드 품질 개선)

---

## 4. 종합 권장사항

### 4.1 즉시 개선 필요 (Critical)

| 항목 | 문제 | 조치 |
|------|------|------|
| 인텐트 분류 우선순위 | 인사말이 RAG보다 우선 처리 | RAG 키워드 우선 체크로 변경 |
| TokenCountEstimator | 주석 처리로 미적용 | Bean 활성화 및 TokenService 연동 |

### 4.2 개선 권장 (Recommended)

| 항목 | 문제 | 조치 |
|------|------|------|
| Prompt Injection | 방지 로직 없음 | 패턴 필터링 추가 |
| 히스토리 중복 로드 | 매 요청 시 전체 재로드 | 증분 추가 방식으로 변경 |
| DTO 변환 | RefinedResult↔SearchResult 중복 | 단일 DTO로 통합 |

### 4.3 향후 고려 (Optional)

| 항목 | 내용 |
|------|------|
| 의문사 패턴 확장 | "뭐", "몇", "얼마" 등 구어체 추가 |
| 키워드 외부화 | 하드코딩된 키워드를 설정 파일로 분리 |
| 실제 토큰 수 수집 | LLM 응답에서 tokenUsage 추출 |

---

## 5. 참고 자료

- MongoDB Atlas Vector Search: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
- langchain4j 공식 문서: https://docs.langchain4j.dev/
- OpenAI Embeddings: https://platform.openai.com/docs/guides/embeddings
- OpenAI Tokenizer: https://platform.openai.com/tokenizer
