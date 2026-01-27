# RAG 기반 멀티턴 채팅 기능 개선 상세 구현 설계서

**작성 일시**: 2026-01-27
**참고 분석 리포트**: `docs/step12/rag-chatbot-analysis-report.md`
**대상 모듈**: `api/chatbot`

---

## 1. 개요

### 1.1 목적

분석 리포트에서 도출된 개선사항들을 실제 구현 가능한 수준으로 상세 설계하여, 코드 수정 시 참조할 수 있는 명확한 가이드를 제공한다.

### 1.2 개선 범위

| 우선순위 | 항목 수 | 영향 파일 수 |
|---------|--------|-------------|
| Critical | 2 | 3 |
| Recommended | 3 | 4 |
| Optional | 3 | 4 |

### 1.3 우선순위별 분류

```
Critical (즉시 개선)
├── 인텐트 분류 우선순위 수정
└── TokenCountEstimator Bean 활성화

Recommended (개선 권장)
├── Prompt Injection 방지 로직 추가
├── 히스토리 중복 로드 최적화
└── DTO 변환 단순화

Optional (향후 고려)
├── 의문사 패턴 확장
├── 키워드 외부화
└── 실제 토큰 수 수집
```

---

## 2. 즉시 개선 항목 (Critical)

### 2.1 인텐트 분류 우선순위 수정

#### 2.1.1 현재 구현

**파일**: `api/chatbot/.../service/IntentClassificationServiceImpl.java`

```java
@Override
public Intent classifyIntent(String preprocessedInput) {
    String lowerInput = preprocessedInput.toLowerCase();

    // 1. 인사말 체크 (우선) ← 문제점
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

#### 2.1.2 문제점 분석

인사말이 RAG 키워드보다 우선 처리되어 복합 의도가 잘못 분류됨.

| 입력 | 현재 결과 | 기대 결과 |
|------|----------|----------|
| "안녕하세요 대회 정보 알려줘" | GENERAL_CONVERSATION | RAG_REQUIRED |
| "hi, what contests are available?" | GENERAL_CONVERSATION | RAG_REQUIRED |
| "안녕하세요" | GENERAL_CONVERSATION | GENERAL_CONVERSATION |

#### 2.1.3 개선 설계

**수정 코드**:

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

**변경 전/후 동작 비교**:

| 입력 | 변경 전 | 변경 후 |
|------|--------|--------|
| "안녕하세요 대회 정보 알려줘" | GENERAL_CONVERSATION | RAG_REQUIRED |
| "hi, what contests are available?" | GENERAL_CONVERSATION | RAG_REQUIRED |
| "안녕하세요" | GENERAL_CONVERSATION | GENERAL_CONVERSATION |
| "대회 정보" | RAG_REQUIRED | RAG_REQUIRED |
| "오늘 뭐해?" | GENERAL_CONVERSATION | RAG_REQUIRED (질문형태) |

#### 2.1.4 테스트 케이스

```java
@Test
void classifyIntent_greetingWithRagKeyword_shouldReturnRagRequired() {
    // given
    String input = "안녕하세요 대회 정보 알려줘";

    // when
    Intent intent = intentService.classifyIntent(input);

    // then
    assertThat(intent).isEqualTo(Intent.RAG_REQUIRED);
}

@Test
void classifyIntent_greetingOnly_shouldReturnGeneralConversation() {
    // given
    String input = "안녕하세요";

    // when
    Intent intent = intentService.classifyIntent(input);

    // then
    assertThat(intent).isEqualTo(Intent.GENERAL_CONVERSATION);
}

@Test
void classifyIntent_ragKeywordOnly_shouldReturnRagRequired() {
    // given
    String input = "최근 Kaggle 대회 알려줘";

    // when
    Intent intent = intentService.classifyIntent(input);

    // then
    assertThat(intent).isEqualTo(Intent.RAG_REQUIRED);
}
```

---

### 2.2 TokenCountEstimator Bean 활성화

#### 2.2.1 현재 구현

**파일**: `api/chatbot/.../config/LangChain4jConfig.java`

```java
// @Bean
// public TokenCountEstimator tokenCountEstimator() {
//     // TODO: ChatMemory 구현 시점에 TokenCountEstimator Bean 추가 필요
//     // return new OpenAiTokenCountEstimator(chatModelName);
// }
```

**파일**: `api/chatbot/.../service/TokenServiceImpl.java`

```java
@Override
public int estimateTokens(String text) {
    int wordCount = text.split("\\s+").length;
    int koreanCharCount = (int) text.chars()
        .filter(c -> c >= 0xAC00 && c <= 0xD7A3)
        .count();

    // 휴리스틱 추정 (부정확)
    int estimatedTokens = (int) (koreanCharCount * 2 + (wordCount - koreanCharCount) * 1.3);
    return Math.max(estimatedTokens, text.length() / 4);
}
```

#### 2.2.2 개선 설계

**LangChain4jConfig.java 수정**:

```java
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.TokenCountEstimator;

@Bean
public TokenCountEstimator tokenCountEstimator() {
    return new OpenAiTokenCountEstimator(chatModelName);
}
```

**TokenServiceImpl.java 수정**:

```java
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private final TokenCountEstimator tokenCountEstimator;

    @Value("${chatbot.token.max-input-tokens:4000}")
    private int maxInputTokens;

    @Value("${chatbot.token.max-output-tokens:2000}")
    private int maxOutputTokens;

    @Value("${chatbot.token.warning-threshold:0.8}")
    private double warningThreshold;

    public TokenServiceImpl(@Autowired(required = false) TokenCountEstimator tokenCountEstimator) {
        this.tokenCountEstimator = tokenCountEstimator;
    }

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // TokenCountEstimator가 있으면 사용, 없으면 fallback
        if (tokenCountEstimator != null) {
            try {
                return tokenCountEstimator.estimateTokenCount(text);
            } catch (Exception e) {
                log.warn("TokenCountEstimator failed, falling back to heuristic: {}", e.getMessage());
                return estimateTokensHeuristic(text);
            }
        }

        return estimateTokensHeuristic(text);
    }

    /**
     * Fallback: 휴리스틱 기반 토큰 추정
     */
    private int estimateTokensHeuristic(String text) {
        int wordCount = text.split("\\s+").length;
        int koreanCharCount = (int) text.chars()
            .filter(c -> c >= 0xAC00 && c <= 0xD7A3)
            .count();

        int estimatedTokens = (int) (koreanCharCount * 2 + (wordCount - koreanCharCount) * 1.3);
        return Math.max(estimatedTokens, text.length() / 4);
    }

    // ... 나머지 메서드는 동일
}
```

#### 2.2.3 Fallback 전략

| 상황 | 동작 |
|------|------|
| TokenCountEstimator Bean 존재 | `tokenCountEstimator.estimateTokenCount()` 사용 |
| TokenCountEstimator Bean 미존재 | 휴리스틱 추정 사용 |
| TokenCountEstimator 예외 발생 | 휴리스틱 추정으로 fallback, 경고 로깅 |

**의존성 확인** (`build.gradle`):

```gradle
// langchain4j-open-ai 모듈에 OpenAiTokenCountEstimator 포함
implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0'
```

---

## 3. 개선 권장 항목 (Recommended)

### 3.1 Prompt Injection 방지

#### 3.1.1 탐지 패턴 정의

```java
private static final Pattern INJECTION_PATTERN = Pattern.compile(
    "(?i)(" +
        "ignore\\s+(previous|all|above)\\s+instructions|" +  // 영어 지시 무시 패턴
        "disregard\\s+(previous|all|above)\\s+instructions|" +
        "<\\|[a-z]+\\|>|" +                                   // OpenAI 특수 토큰
        "\\[INST\\]|\\[/INST\\]|" +                           // Llama 특수 토큰
        "<<SYS>>|<</SYS>>|" +                                 // Llama 시스템 토큰
        "###\\s*(instruction|system)|" +                      // 마크다운 헤더 주입
        "(?:^|\\s)system:\\s*" +                              // 시스템 역할 주입
    ")"
);
```

#### 3.1.2 처리 정책

| 정책 | 동작 | 적용 환경 |
|------|------|----------|
| 로깅 | 탐지 시 WARN 로깅 후 계속 처리 | 개발/테스트 |
| 필터링 | 탐지된 패턴을 제거하고 처리 | 스테이징 |
| 거부 | 탐지 시 예외 발생, 요청 거부 | 운영 (선택적) |

**권장**: 로깅 정책 (탐지 후 계속 처리)

#### 3.1.3 구현 코드

**파일**: `api/chatbot/.../service/InputPreprocessingServiceImpl.java`

```java
@Slf4j
@Service
public class InputPreprocessingServiceImpl implements InputPreprocessingService {

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
        "(?i)(" +
            "ignore\\s+(previous|all|above)\\s+instructions|" +
            "disregard\\s+(previous|all|above)\\s+instructions|" +
            "<\\|[a-z]+\\|>|" +
            "\\[INST\\]|\\[/INST\\]|" +
            "<<SYS>>|<</SYS>>|" +
            "###\\s*(instruction|system)|" +
            "(?:^|\\s)system:\\s*" +
        ")"
    );

    @Value("${chatbot.input.max-length:500}")
    private int maxLength;

    @Value("${chatbot.input.min-length:1}")
    private int minLength;

    @Override
    public PreprocessedInput preprocess(String rawInput) {
        // 1. Null 및 빈 문자열 검증
        if (rawInput == null || rawInput.isBlank()) {
            throw new InvalidInputException("입력이 비어있습니다.");
        }

        // 2. 길이 검증
        if (rawInput.length() > maxLength) {
            throw new InvalidInputException(
                String.format("입력 길이는 %d자를 초과할 수 없습니다.", maxLength)
            );
        }
        if (rawInput.length() < minLength) {
            throw new InvalidInputException(
                String.format("입력 길이는 최소 %d자 이상이어야 합니다.", minLength)
            );
        }

        // 3. Prompt Injection 탐지 (로깅 정책)
        detectPromptInjection(rawInput);

        // 4. 정규화
        String normalized = normalize(rawInput);

        // 5. 특수 문자 필터링
        String cleaned = cleanSpecialCharacters(normalized);

        return PreprocessedInput.builder()
            .original(rawInput)
            .normalized(normalized)
            .cleaned(cleaned)
            .length(cleaned.length())
            .build();
    }

    /**
     * Prompt Injection 탐지 (로깅 정책)
     */
    private void detectPromptInjection(String input) {
        Matcher matcher = INJECTION_PATTERN.matcher(input);
        if (matcher.find()) {
            log.warn("Potential prompt injection detected. Pattern: '{}', Input length: {}",
                matcher.group(), input.length());
        }
    }

    // ... 기존 메서드 유지
}
```

---

### 3.2 히스토리 로드 최적화

#### 3.2.1 현재 구현의 문제

**파일**: `api/chatbot/.../service/ChatbotServiceImpl.java`

```java
// 문제 1: loadHistoryIfExists에서 전체 히스토리 로드
private void loadHistoryIfExists(ChatRequest request, String sessionId, ChatMemory chatMemory) {
    if (request.conversationId() != null && !request.conversationId().isBlank()) {
        List<ChatMessage> history = messageService.getMessagesForMemory(sessionId, null);
        history.forEach(chatMemory::add);  // 전체 히스토리 로드
    }
}

// 문제 2: saveMessagesToMemory에서 다시 전체 히스토리 로드
private void saveMessagesToMemory(String sessionId, String userMessage, String assistantMessage) {
    ChatMemory chatMemory = memoryProvider.get(sessionId);
    List<ChatMessage> history = messageService.getMessagesForMemory(sessionId, null);
    history.forEach(chatMemory::add);  // 중복 로드!

    // 현재 메시지 추가
    chatMemory.add(UserMessage.from(userMessage));
    chatMemory.add(AiMessage.from(assistantMessage));

    // DB 저장
    messageService.saveMessage(sessionId, "USER", userMessage, ...);
    messageService.saveMessage(sessionId, "ASSISTANT", assistantMessage, ...);
}
```

**문제점**:
1. `loadHistoryIfExists`에서 이미 로드한 히스토리를 `saveMessagesToMemory`에서 다시 로드
2. RAG 경로에서 `loadHistoryIfExists` 호출 없이 `saveMessagesToMemory`만 호출되어 중복 로드 발생
3. ChatMemory에 동일한 메시지가 중복 추가될 수 있음

#### 3.2.2 개선 설계

```java
@Override
public ChatResponse generateResponse(ChatRequest request, Long userId) {
    String sessionId = getOrCreateSession(request, userId);
    ChatMemory chatMemory = memoryProvider.get(sessionId);

    // 세션이 기존 것이면 히스토리 로드 (한 번만)
    boolean isExistingSession = request.conversationId() != null && !request.conversationId().isBlank();
    if (isExistingSession) {
        loadHistoryToMemory(sessionId, chatMemory);
    }

    Intent intent = intentService.classifyIntent(request.message());

    String response;
    List<SourceResponse> sources;

    if (intent == Intent.GENERAL_CONVERSATION) {
        response = handleGeneralConversation(request, sessionId, chatMemory);
        sources = Collections.emptyList();
    } else {
        RAGResult ragResult = handleRAGPipeline(request, sessionId, userId);
        response = ragResult.response();
        sources = ragResult.sources();
    }

    // 현재 메시지만 저장 (히스토리 재로드 없이)
    saveCurrentMessages(sessionId, chatMemory, request.message(), response);

    sessionService.updateLastMessageAt(sessionId);
    trackTokenUsage(sessionId, userId, request.message(), response);

    return ChatResponse.builder()
        .response(response)
        .conversationId(sessionId)
        .sources(sources)
        .build();
}

/**
 * 히스토리를 ChatMemory에 로드 (한 번만 호출)
 */
private void loadHistoryToMemory(String sessionId, ChatMemory chatMemory) {
    List<ChatMessage> history = messageService.getMessagesForMemory(sessionId, null);
    history.forEach(chatMemory::add);
}

/**
 * 현재 메시지만 저장 (히스토리 재로드 없이)
 */
private void saveCurrentMessages(String sessionId, ChatMemory chatMemory,
                                  String userMessage, String assistantMessage) {
    // ChatMemory에 추가 (이미 히스토리는 로드됨)
    chatMemory.add(UserMessage.from(userMessage));
    chatMemory.add(AiMessage.from(assistantMessage));

    // DB에만 저장
    messageService.saveMessage(sessionId, "USER", userMessage,
        tokenService.estimateTokens(userMessage));
    messageService.saveMessage(sessionId, "ASSISTANT", assistantMessage,
        tokenService.estimateTokens(assistantMessage));
}

/**
 * 일반 대화 처리 (chatMemory 파라미터로 전달받음)
 */
private String handleGeneralConversation(ChatRequest request, String sessionId,
                                          ChatMemory chatMemory) {
    // chatMemory는 이미 히스토리가 로드된 상태
    UserMessage userMessage = UserMessage.from(request.message());
    chatMemory.add(userMessage);

    List<ChatMessage> messages = chatMemory.messages();
    Object providerFormat = messageConverter.convertToProviderFormat(messages, null);
    String response = llmService.generate(providerFormat.toString());

    // ChatMemory에 응답 추가는 saveCurrentMessages에서 처리하므로 여기서는 생략
    // 단, handleGeneralConversation 내에서 이미 추가했으므로 saveCurrentMessages 호출 시 중복 방지 필요

    return response;
}
```

**수정 후 흐름**:

```
1. generateResponse 호출
   ↓
2. 기존 세션이면 loadHistoryToMemory (한 번만)
   ↓
3. 의도 분류 및 분기 처리
   ↓
4. saveCurrentMessages (현재 메시지만 저장, 히스토리 재로드 없음)
```

---

### 3.3 DTO 변환 단순화

#### 3.3.1 현재 DTO 구조

**SearchResult.java**:
```java
@Builder
public record SearchResult(
    String documentId,
    String text,
    Double score,
    String collectionType,
    Object metadata
) {}
```

**RefinedResult.java**:
```java
@Builder
public record RefinedResult(
    String documentId,
    String text,
    Double score,
    String collectionType,
    Object metadata
) {}
```

**문제점**: 두 DTO가 완전히 동일한 필드 구조를 가짐.

#### 3.3.2 개선 방안

**방안 A: 단일 DTO 사용 (권장)**

`RefinedResult` 삭제 후 `SearchResult`만 사용.

```java
// ResultRefinementChain.java 수정
public List<SearchResult> refine(List<SearchResult> rawResults) {
    // 1. 중복 제거 (유사도 필터링은 VectorSearchService에서 이미 수행)
    List<SearchResult> deduplicated = removeDuplicates(rawResults);

    // 2. 관련성 순으로 정렬
    return deduplicated.stream()
        .sorted(Comparator.comparing(SearchResult::score).reversed())
        .collect(Collectors.toList());
}

// AnswerGenerationChain.java 수정
public String generate(String query, List<SearchResult> searchResults) {
    // 불필요한 변환 제거
    String prompt = promptService.buildPrompt(query, searchResults);
    String answer = llmService.generate(prompt);
    return postProcess(answer);
}
```

**방안 B: 공통 인터페이스 추출**

```java
public interface DocumentResult {
    String documentId();
    String text();
    Double score();
    String collectionType();
    Object metadata();
}

@Builder
public record SearchResult(...) implements DocumentResult {}

@Builder
public record RefinedResult(...) implements DocumentResult {}
```

**권장**: 방안 A (단일 DTO 사용)

#### 3.3.3 영향 범위

| 파일 | 수정 내용 |
|------|----------|
| `RefinedResult.java` | 삭제 |
| `ResultRefinementChain.java` | 반환 타입 `List<SearchResult>`로 변경 |
| `AnswerGenerationChain.java` | 파라미터 타입 변경, 변환 로직 제거 |
| `ChatbotServiceImpl.java` | `refinedResults` 타입 변경 |

---

## 4. 향후 고려 항목 (Optional)

### 4.1 의문사 패턴 확장

**현재**:
```java
boolean hasQuestionWords = input.matches(".*(무엇|어떤|어디|언제|누가|왜|어떻게).*");
```

**확장**:
```java
private static final Pattern QUESTION_PATTERN = Pattern.compile(
    ".*(무엇|어떤|어디|언제|누가|왜|어떻게|뭐|몇|얼마|어느|어떻게|어찌).*"
);

private boolean isQuestion(String input) {
    boolean hasQuestionWords = QUESTION_PATTERN.matcher(input).matches();
    boolean hasQuestionMark = input.contains("?") || input.contains("？");
    return hasQuestionWords || hasQuestionMark;
}
```

### 4.2 키워드 외부화

**application-chatbot.yml**:
```yaml
chatbot:
  intent:
    greeting-keywords:
      - 안녕
      - 안녕하세요
      - 하이
      - hi
      - hello
      - 헬로
    rag-keywords:
      - 대회
      - contest
      - 뉴스
      - news
      - 기사
      - 아카이브
      - archive
      - 검색
      - 찾아
      - 알려
      - 정보
```

**IntentKeywordsProperties.java**:
```java
@ConfigurationProperties(prefix = "chatbot.intent")
public record IntentKeywordsProperties(
    List<String> greetingKeywords,
    List<String> ragKeywords
) {}
```

### 4.3 실제 토큰 수 수집

**LLMResponse.java**:
```java
public record LLMResponse(
    String content,
    int inputTokens,
    int outputTokens
) {}
```

**LLMServiceImpl.java 수정**:
```java
public LLMResponse generateWithTokenUsage(String prompt) {
    try {
        Response<AiMessage> response = chatLanguageModel.generate(
            UserMessage.from(prompt)
        );
        TokenUsage usage = response.tokenUsage();

        return new LLMResponse(
            response.content().text(),
            usage != null ? usage.inputTokenCount() : 0,
            usage != null ? usage.outputTokenCount() : 0
        );
    } catch (Exception e) {
        log.error("Failed to generate LLM response", e);
        throw new RuntimeException("LLM 응답 생성 실패", e);
    }
}
```

---

## 5. 구현 순서

| 순서 | 항목 | 수정 파일 | 우선순위 | 예상 복잡도 |
|------|------|----------|---------|-----------|
| 1 | 인텐트 분류 우선순위 수정 | `IntentClassificationServiceImpl.java` | Critical | Low |
| 2 | TokenCountEstimator 활성화 | `LangChain4jConfig.java`, `TokenServiceImpl.java` | Critical | Low |
| 3 | Prompt Injection 방지 | `InputPreprocessingServiceImpl.java` | Recommended | Low |
| 4 | 히스토리 로드 최적화 | `ChatbotServiceImpl.java` | Recommended | Medium |
| 5 | DTO 변환 단순화 | `ResultRefinementChain.java`, `AnswerGenerationChain.java`, `RefinedResult.java` | Recommended | Medium |
| 6 | 의문사 패턴 확장 | `IntentClassificationServiceImpl.java` | Optional | Low |
| 7 | 키워드 외부화 | 신규 파일 + `application.yml` | Optional | Medium |
| 8 | 실제 토큰 수 수집 | `LLMServiceImpl.java`, 신규 DTO | Optional | Medium |

---

## 6. 참고 자료

- langchain4j 공식 문서: https://docs.langchain4j.dev/
- langchain4j GitHub: https://github.com/langchain4j/langchain4j
- OpenAI Tokenizer: https://platform.openai.com/tokenizer
- MongoDB Atlas Vector Search: https://www.mongodb.com/docs/atlas/atlas-vector-search/
