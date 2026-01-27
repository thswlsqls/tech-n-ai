# RAG 챗봇 Intent 라우팅 개선 상세 설계서

**작성 일시**: 2026-01-27
**대상 모듈**: `api/chatbot`
**참고 문서**:
- `docs/step12/rag-chatbot-analysis-report.md`
- `docs/step12/rag-chatbot-improvement-design.md`
- `prompts/step12/rag-chatbot-routing-reranking-improvement-prompt.md`

---

## 1. 현재 구현 분석

### 1.1 현재 Intent Enum

**파일**: `api/chatbot/.../service/dto/Intent.java`

```java
public enum Intent {
    RAG_REQUIRED,           // RAG 필요
    GENERAL_CONVERSATION    // 일반 대화
}
```

### 1.2 현재 분류 로직

**파일**: `api/chatbot/.../service/IntentClassificationServiceImpl.java`

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

### 1.3 현재 라우팅 로직

**파일**: `api/chatbot/.../service/ChatbotServiceImpl.java`

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

### 1.4 문제점

| 항목 | 문제점 | 영향 |
|------|--------|------|
| Intent 타입 부족 | Web 검색 Intent 미정의 | 최신/실시간 정보 요청 시 RAG로 잘못 분류 |
| 분류 로직 한계 | Web 검색 판단 로직 없음 | 날씨, 주가 등 외부 정보 요청 처리 불가 |
| 라우팅 경로 부족 | 2가지 경로만 존재 | Web 검색 파이프라인 실행 불가 |

---

## 2. 개선 목표

1. **Intent Enum 확장**: `WEB_SEARCH_REQUIRED` Intent 추가
2. **분류 로직 개선**: Web 검색 키워드 및 패턴 기반 분류 로직 추가
3. **라우팅 로직 수정**: 3가지 경로(LLM 직접, RAG, Web 검색) 분기 처리
4. **Web 검색 서비스 설계**: 외부 검색 API 연동 서비스 구현

---

## 3. 상세 설계

### 3.1 Intent Enum 확장

**파일**: `api/chatbot/.../service/dto/Intent.java`

```java
package com.tech.n.ai.api.chatbot.service.dto;

/**
 * 의도 분류 결과
 */
public enum Intent {
    /**
     * LLM 직접 요청
     * - 일반 대화, 인사말
     * - 창작 요청 (작성해줘, 만들어줘)
     * - 번역, 요약 등 텍스트 처리
     */
    LLM_DIRECT,

    /**
     * RAG 요청 (내부 데이터 검색 필요)
     * - 대회, 뉴스, 아카이브 관련 질문
     * - 저장된 데이터 기반 답변 필요
     */
    RAG_REQUIRED,

    /**
     * Web 검색 요청 (최신/실시간 정보 필요)
     * - 날씨, 주가, 환율 등 실시간 정보
     * - 최신 뉴스, 현재 이벤트
     * - 시간 민감성 질문
     */
    WEB_SEARCH_REQUIRED
}
```

### 3.2 Intent 분류 로직 개선

**파일**: `api/chatbot/.../service/IntentClassificationServiceImpl.java`

```java
package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class IntentClassificationServiceImpl implements IntentClassificationService {

    private static final Set<String> GREETING_KEYWORDS = Set.of(
        "안녕", "안녕하세요", "하이", "hi", "hello", "헬로"
    );

    private static final Set<String> RAG_KEYWORDS = Set.of(
        "대회", "contest", "뉴스", "news", "기사", "아카이브", "archive",
        "검색", "찾아", "알려", "정보", "어떤", "무엇",
        "kaggle", "codeforces", "leetcode", "hackathon"
    );

    /**
     * Web 검색이 필요한 최신/실시간 정보 키워드
     */
    private static final Set<String> WEB_SEARCH_KEYWORDS = Set.of(
        // 시간 관련 키워드
        "오늘", "현재", "지금", "최근", "today", "now", "latest", "current",
        // 실시간 정보 키워드
        "날씨", "weather", "주가", "stock", "환율", "exchange rate",
        "뉴스 속보", "breaking news", "실시간",
        // 외부 정보 요청
        "검색해줘", "찾아줘", "인터넷에서"
    );

    /**
     * 창작/텍스트 처리 요청 키워드 (LLM 직접 처리)
     */
    private static final Set<String> LLM_DIRECT_KEYWORDS = Set.of(
        "작성해줘", "만들어줘", "써줘", "번역", "요약", "설명해줘",
        "write", "create", "translate", "summarize", "explain"
    );

    @Override
    public Intent classifyIntent(String preprocessedInput) {
        String lowerInput = preprocessedInput.toLowerCase();

        // 1. Web 검색 키워드 체크 (최우선)
        if (containsWebSearchKeywords(lowerInput)) {
            log.debug("Intent classified as WEB_SEARCH_REQUIRED for input: {}",
                truncateForLog(preprocessedInput));
            return Intent.WEB_SEARCH_REQUIRED;
        }

        // 2. RAG 키워드 체크
        if (containsRagKeywords(lowerInput)) {
            log.debug("Intent classified as RAG_REQUIRED for input: {}",
                truncateForLog(preprocessedInput));
            return Intent.RAG_REQUIRED;
        }

        // 3. 질문 형태 체크 (RAG 관련 질문일 가능성)
        if (isQuestion(lowerInput) && !containsLlmDirectKeywords(lowerInput)) {
            log.debug("Intent classified as RAG_REQUIRED (question form) for input: {}",
                truncateForLog(preprocessedInput));
            return Intent.RAG_REQUIRED;
        }

        // 4. 인사말 또는 LLM 직접 처리 키워드
        log.debug("Intent classified as LLM_DIRECT for input: {}",
            truncateForLog(preprocessedInput));
        return Intent.LLM_DIRECT;
    }

    private boolean isGreeting(String input) {
        return GREETING_KEYWORDS.stream()
            .anyMatch(input::contains);
    }

    private boolean containsRagKeywords(String input) {
        return RAG_KEYWORDS.stream()
            .anyMatch(input::contains);
    }

    private boolean containsWebSearchKeywords(String input) {
        return WEB_SEARCH_KEYWORDS.stream()
            .anyMatch(input::contains);
    }

    private boolean containsLlmDirectKeywords(String input) {
        return LLM_DIRECT_KEYWORDS.stream()
            .anyMatch(input::contains);
    }

    private boolean isQuestion(String input) {
        // 의문사 체크 (구어체 포함)
        boolean hasQuestionWords = input.matches(
            ".*(무엇|어떤|어디|언제|누가|왜|어떻게|뭐|몇|얼마|어느).*"
        );

        // 물음표 체크
        boolean hasQuestionMark = input.contains("?") || input.contains("？");

        return hasQuestionWords || hasQuestionMark;
    }

    private String truncateForLog(String input) {
        return input.length() > 50 ? input.substring(0, 50) + "..." : input;
    }
}
```

### 3.3 ChatbotService 라우팅 로직 수정

**파일**: `api/chatbot/.../service/ChatbotServiceImpl.java`

```java
@Override
public ChatResponse generateResponse(ChatRequest request, Long userId) {
    String sessionId = getOrCreateSession(request, userId);
    ChatMemory chatMemory = memoryProvider.get(sessionId);

    // 기존 세션이면 히스토리 로드 (한 번만)
    boolean isExistingSession = request.conversationId() != null && !request.conversationId().isBlank();
    if (isExistingSession) {
        loadHistoryToMemory(sessionId, chatMemory);
    }

    Intent intent = intentService.classifyIntent(request.message());

    String response;
    List<SourceResponse> sources;

    // 3가지 경로 분기 처리
    switch (intent) {
        case LLM_DIRECT:
            response = handleLlmDirect(request, sessionId, chatMemory);
            sources = Collections.emptyList();
            break;

        case RAG_REQUIRED:
            RAGResult ragResult = handleRAGPipeline(request, sessionId, userId);
            response = ragResult.response();
            sources = ragResult.sources();
            break;

        case WEB_SEARCH_REQUIRED:
            WebSearchResult webResult = handleWebSearchPipeline(request, sessionId);
            response = webResult.response();
            sources = webResult.sources();
            break;

        default:
            response = handleLlmDirect(request, sessionId, chatMemory);
            sources = Collections.emptyList();
    }

    // 현재 메시지만 저장
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
 * LLM 직접 호출 처리 (기존 handleGeneralConversation 리네이밍)
 */
private String handleLlmDirect(ChatRequest request, String sessionId, ChatMemory chatMemory) {
    UserMessage userMessage = UserMessage.from(request.message());
    chatMemory.add(userMessage);

    List<ChatMessage> messages = chatMemory.messages();
    Object providerFormat = messageConverter.convertToProviderFormat(messages, null);
    String response = llmService.generate(providerFormat.toString());

    AiMessage aiMessage = AiMessage.from(response);
    chatMemory.add(aiMessage);

    return response;
}

/**
 * Web 검색 파이프라인 처리
 */
private WebSearchResult handleWebSearchPipeline(ChatRequest request, String sessionId) {
    // 1. Web 검색 실행
    List<WebSearchDocument> searchResults = webSearchService.search(request.message());

    // 2. 검색 결과 기반 프롬프트 생성 및 LLM 호출
    String prompt = promptService.buildWebSearchPrompt(request.message(), searchResults);
    String response = llmService.generate(prompt);

    // 3. 출처 정보 생성
    List<SourceResponse> sources = searchResults.stream()
        .map(doc -> SourceResponse.builder()
            .documentId(doc.url())
            .collectionType("WEB")
            .title(doc.title())
            .url(doc.url())
            .build())
        .collect(Collectors.toList());

    return new WebSearchResult(response, sources);
}

private record WebSearchResult(String response, List<SourceResponse> sources) {}
```

### 3.4 Web 검색 서비스 설계

#### 3.4.1 WebSearchService 인터페이스

**파일**: `api/chatbot/.../service/WebSearchService.java`

```java
package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.WebSearchDocument;
import java.util.List;

/**
 * Web 검색 서비스 인터페이스
 */
public interface WebSearchService {

    /**
     * 쿼리로 Web 검색 수행
     *
     * @param query 검색 쿼리
     * @return 검색 결과 문서 목록
     */
    List<WebSearchDocument> search(String query);

    /**
     * 쿼리로 Web 검색 수행 (결과 수 제한)
     *
     * @param query 검색 쿼리
     * @param maxResults 최대 결과 수
     * @return 검색 결과 문서 목록
     */
    List<WebSearchDocument> search(String query, int maxResults);
}
```

#### 3.4.2 WebSearchDocument DTO

**파일**: `api/chatbot/.../service/dto/WebSearchDocument.java`

```java
package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * Web 검색 결과 문서
 */
@Builder
public record WebSearchDocument(
    String title,
    String url,
    String snippet,
    String source
) {}
```

#### 3.4.3 WebSearchServiceImpl 구현체 (Google Custom Search API 사용)

**파일**: `api/chatbot/.../service/WebSearchServiceImpl.java`

```java
package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.WebSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchServiceImpl implements WebSearchService {

    private final RestTemplate restTemplate;

    @Value("${chatbot.web-search.enabled:false}")
    private boolean enabled;

    @Value("${chatbot.web-search.api-key:}")
    private String apiKey;

    @Value("${chatbot.web-search.search-engine-id:}")
    private String searchEngineId;

    @Value("${chatbot.web-search.max-results:5}")
    private int defaultMaxResults;

    private static final String GOOGLE_SEARCH_API_URL =
        "https://www.googleapis.com/customsearch/v1?key={apiKey}&cx={cx}&q={query}&num={num}";

    @Override
    public List<WebSearchDocument> search(String query) {
        return search(query, defaultMaxResults);
    }

    @Override
    public List<WebSearchDocument> search(String query, int maxResults) {
        if (!enabled) {
            log.warn("Web search is disabled. Returning empty results.");
            return Collections.emptyList();
        }

        if (apiKey.isBlank() || searchEngineId.isBlank()) {
            log.error("Web search API key or search engine ID is not configured.");
            return Collections.emptyList();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                GOOGLE_SEARCH_API_URL,
                Map.class,
                apiKey, searchEngineId, query, maxResults
            );

            return parseSearchResults(response);
        } catch (Exception e) {
            log.error("Web search failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<WebSearchDocument> parseSearchResults(Map<String, Object> response) {
        if (response == null || !response.containsKey("items")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        return items.stream()
            .map(item -> WebSearchDocument.builder()
                .title((String) item.get("title"))
                .url((String) item.get("link"))
                .snippet((String) item.get("snippet"))
                .source((String) item.get("displayLink"))
                .build())
            .toList();
    }
}
```

### 3.5 PromptService 확장

**파일**: `api/chatbot/.../service/PromptService.java`

```java
/**
 * Web 검색 결과 기반 프롬프트 생성
 *
 * @param query 사용자 쿼리
 * @param searchResults Web 검색 결과
 * @return 생성된 프롬프트
 */
String buildWebSearchPrompt(String query, List<WebSearchDocument> searchResults);
```

**파일**: `api/chatbot/.../service/PromptServiceImpl.java`

```java
@Override
public String buildWebSearchPrompt(String query, List<WebSearchDocument> searchResults) {
    StringBuilder context = new StringBuilder();
    context.append("다음은 웹 검색 결과입니다:\n\n");

    for (int i = 0; i < searchResults.size(); i++) {
        WebSearchDocument doc = searchResults.get(i);
        context.append(String.format("[%d] %s\n", i + 1, doc.title()));
        context.append(String.format("출처: %s\n", doc.source()));
        context.append(String.format("내용: %s\n\n", doc.snippet()));
    }

    return String.format("""
        당신은 도움이 되는 AI 어시스턴트입니다.

        %s

        위의 검색 결과를 참고하여 다음 질문에 답변해주세요.
        답변 시 출처를 명시해주세요.

        질문: %s
        """, context.toString(), query);
}
```

### 3.6 SourceResponse DTO 확장

**파일**: `api/chatbot/.../dto/response/SourceResponse.java`

```java
package com.tech.n.ai.api.chatbot.dto.response;

import lombok.Builder;

@Builder
public record SourceResponse(
    String documentId,
    String collectionType,
    Double score,
    // Web 검색 결과용 추가 필드
    String title,
    String url
) {}
```

---

## 4. 설정 파일 변경사항

### 4.1 application-chatbot.yml

```yaml
chatbot:
  # 기존 설정 유지...

  # Web 검색 설정 추가
  web-search:
    enabled: false  # 기본값: 비활성화
    api-key: ${GOOGLE_SEARCH_API_KEY:}
    search-engine-id: ${GOOGLE_SEARCH_ENGINE_ID:}
    max-results: 5
```

### 4.2 RestTemplate Bean 추가

**파일**: `api/chatbot/.../config/WebSearchConfig.java`

```java
package com.tech.n.ai.api.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebSearchConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

## 5. 의존성

### 5.1 필수 의존성 (기존 유지)

```gradle
// build.gradle - 추가 의존성 없음
// RestTemplate은 Spring Web에 포함
```

### 5.2 외부 API

| API | 용도 | 공식 문서 |
|-----|------|----------|
| Google Custom Search API | Web 검색 | https://developers.google.com/custom-search/v1/overview |

---

## 6. 구현 순서

| 순서 | 항목 | 수정 파일 | 복잡도 |
|------|------|----------|--------|
| 1 | Intent Enum 확장 | `Intent.java` | Low |
| 2 | WebSearchDocument DTO 생성 | 신규 파일 | Low |
| 3 | SourceResponse 확장 | `SourceResponse.java` | Low |
| 4 | IntentClassificationService 수정 | `IntentClassificationServiceImpl.java` | Low |
| 5 | WebSearchService 인터페이스 생성 | 신규 파일 | Low |
| 6 | WebSearchServiceImpl 구현 | 신규 파일 | Medium |
| 7 | WebSearchConfig 생성 | 신규 파일 | Low |
| 8 | PromptService 확장 | `PromptService.java`, `PromptServiceImpl.java` | Low |
| 9 | ChatbotServiceImpl 수정 | `ChatbotServiceImpl.java` | Medium |
| 10 | 설정 파일 수정 | `application-chatbot.yml` | Low |

---

## 7. 테스트 케이스

### 7.1 Intent 분류 테스트

```java
@Test
void classifyIntent_webSearchKeyword_shouldReturnWebSearchRequired() {
    // given
    String input = "오늘 날씨 어때?";

    // when
    Intent intent = intentService.classifyIntent(input);

    // then
    assertThat(intent).isEqualTo(Intent.WEB_SEARCH_REQUIRED);
}

@Test
void classifyIntent_ragKeyword_shouldReturnRagRequired() {
    // given
    String input = "Kaggle 대회 정보 알려줘";

    // when
    Intent intent = intentService.classifyIntent(input);

    // then
    assertThat(intent).isEqualTo(Intent.RAG_REQUIRED);
}

@Test
void classifyIntent_greeting_shouldReturnLlmDirect() {
    // given
    String input = "안녕하세요";

    // when
    Intent intent = intentService.classifyIntent(input);

    // then
    assertThat(intent).isEqualTo(Intent.LLM_DIRECT);
}

@Test
void classifyIntent_createRequest_shouldReturnLlmDirect() {
    // given
    String input = "이력서 작성해줘";

    // when
    Intent intent = intentService.classifyIntent(input);

    // then
    assertThat(intent).isEqualTo(Intent.LLM_DIRECT);
}
```

### 7.2 Web 검색 서비스 테스트

```java
@Test
void search_whenDisabled_shouldReturnEmptyList() {
    // given
    ReflectionTestUtils.setField(webSearchService, "enabled", false);

    // when
    List<WebSearchDocument> results = webSearchService.search("test query");

    // then
    assertThat(results).isEmpty();
}

@Test
void search_whenEnabled_shouldReturnResults() {
    // given
    ReflectionTestUtils.setField(webSearchService, "enabled", true);
    // Mock RestTemplate response...

    // when
    List<WebSearchDocument> results = webSearchService.search("날씨");

    // then
    assertThat(results).isNotEmpty();
}
```

### 7.3 라우팅 통합 테스트

```java
@Test
void generateResponse_webSearchIntent_shouldUseWebSearchPipeline() {
    // given
    ChatRequest request = new ChatRequest("오늘 서울 날씨 어때?", null);
    Long userId = 1L;

    // when
    ChatResponse response = chatbotService.generateResponse(request, userId);

    // then
    assertThat(response.sources()).isNotEmpty();
    assertThat(response.sources().get(0).collectionType()).isEqualTo("WEB");
}
```

---

## 8. 참고 자료

### 8.1 프로젝트 내부 문서

- `docs/step12/rag-chatbot-design.md`: 기존 RAG 챗봇 설계서
- `docs/step12/rag-chatbot-analysis-report.md`: 기존 분석 리포트
- `docs/step12/rag-chatbot-improvement-design.md`: 기존 개선 설계서

### 8.2 공식 문서

- Google Custom Search API: https://developers.google.com/custom-search/v1/overview
- Spring RestTemplate: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html
