# Phase 2: LangChain4j Tool 래퍼 설계서

## 1. 개요

### 1.1 목적
Phase 1에서 구축한 데이터 수집 파이프라인의 기능들을 LangChain4j Tool로 래핑하여 AI Agent가 호출할 수 있도록 한다.

### 1.2 전제 조건
- Phase 1 완료: EmergingTech 엔티티, API 엔드포인트, Batch Job 구축
- `api-agent` 모듈에 langchain4j 의존성 설정

---

## 2. 의존성 설정

### 2.1 api-agent/build.gradle
```gradle
implementation 'dev.langchain4j:langchain4j:0.35.0'
implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0'

// 클라이언트 모듈 의존성
implementation project(':client-feign')
implementation project(':client-rss')
implementation project(':client-scraper')
implementation project(':client-slack')
```

---

## 3. Tool 목록 및 인터페이스 설계

### 3.1 Tool 목록

| Tool Name | 설명 | 기존 클라이언트 |
|-----------|------|----------------|
| `fetch_github_releases` | GitHub 저장소 릴리스 조회 | GitHubContract |
| `scrape_web_page` | 웹 페이지 크롤링 | WebScraper |
| `search_emerging_techs` | 저장된 Emerging Tech 업데이트 검색 | EmergingTechService |
| `create_draft_post` | Draft 포스트 생성 | EmergingTechInternalContract |
| `publish_post` | 포스트 게시 (승인) | EmergingTechInternalContract |
| `send_slack_notification` | Slack 알림 발송 | SlackContract |

### 3.2 Tool DTO 설계

```java
// 공통 Result DTO
public record ToolResult(
    boolean success,
    String message,
    Object data
) {
    public static ToolResult success(String message) {
        return new ToolResult(true, message, null);
    }

    public static ToolResult success(String message, Object data) {
        return new ToolResult(true, message, data);
    }

    public static ToolResult failure(String message) {
        return new ToolResult(false, message, null);
    }
}

// GitHub Release DTO (Tool 응답용)
public record GitHubReleaseDto(
    String tagName,
    String name,
    String body,
    String htmlUrl,
    String publishedAt
) {}

// Emerging Tech DTO (Tool 응답용)
public record EmergingTechDto(
    String id,
    String provider,
    String updateType,
    String title,
    String url,
    String status
) {}

// Scraped Content DTO (Tool 응답용)
public record ScrapedContentDto(
    String title,
    String content,
    String url
) {}
```

---

## 4. Tool 클래스 설계

### 4.1 디렉토리 구조

```
api/agent/src/main/java/.../agent/
├── tool/
│   ├── EmergingTechAgentTools.java      // Tool 메서드 정의
│   ├── dto/
│   │   ├── ToolResult.java
│   │   ├── GitHubReleaseDto.java
│   │   ├── EmergingTechDto.java
│   │   └── ScrapedContentDto.java
│   ├── adapter/
│   │   ├── GitHubToolAdapter.java        // GitHubContract 래핑
│   │   ├── ScraperToolAdapter.java       // WebScraper 래핑
│   │   ├── SlackToolAdapter.java         // SlackContract 래핑
│   │   └── EmergingTechToolAdapter.java  // EmergingTechInternalContract 래핑
│   ├── validation/
│   │   └── ToolInputValidator.java       // Tool 입력값 검증
│   └── handler/
│       └── ToolErrorHandlers.java        // Tool 에러 처리
```

### 4.2 Tool 클래스 구현

```java
package com.ebson.shrimp.tm.demo.api.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmergingTechAgentTools {

    private final GitHubToolAdapter githubAdapter;
    private final ScraperToolAdapter scraperAdapter;
    private final SlackToolAdapter slackAdapter;
    private final EmergingTechToolAdapter emergingTechAdapter;

    private final AtomicInteger toolCallCount = new AtomicInteger(0);
    private final AtomicInteger postsCreatedCount = new AtomicInteger(0);
    private final AtomicInteger validationErrorCount = new AtomicInteger(0);

    public void resetCounters() {
        toolCallCount.set(0);
        postsCreatedCount.set(0);
        validationErrorCount.set(0);
    }

    public int getToolCallCount() { return toolCallCount.get(); }
    public int getPostsCreatedCount() { return postsCreatedCount.get(); }
    public int getValidationErrorCount() { return validationErrorCount.get(); }

    @Tool(name = "fetch_github_releases",
          value = "GitHub 저장소의 최신 릴리스 목록을 가져옵니다. SDK 업데이트 확인에 사용합니다.")
    public List<GitHubReleaseDto> fetchGitHubReleases(
        @P("저장소 소유자 (예: openai, anthropics)") String owner,
        @P("저장소 이름 (예: openai-python, anthropic-sdk-python)") String repo
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: fetch_github_releases(owner={}, repo={})", owner, repo);

        String validationError = ToolInputValidator.validateGitHubRepo(owner, repo);
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
            log.warn("Tool 입력값 검증 실패: {}", validationError);
            return List.of();
        }

        return githubAdapter.getReleases(owner, repo);
    }

    @Tool(name = "scrape_web_page",
          value = "웹 페이지를 크롤링하여 텍스트 내용을 추출합니다. 블로그 포스트 내용 확인에 사용합니다.")
    public ScrapedContentDto scrapeWebPage(
        @P("크롤링할 웹 페이지 URL") String url
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: scrape_web_page(url={})", url);

        String validationError = ToolInputValidator.validateUrl(url);
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
            log.warn("Tool 입력값 검증 실패: {}", validationError);
            return new ScrapedContentDto(null, validationError, url);
        }

        return scraperAdapter.scrape(url);
    }

    @Tool(name = "search_emerging_techs",
          value = "저장된 Emerging Tech 업데이트를 검색합니다. 중복 확인이나 기존 데이터 조회에 사용합니다.")
    public List<EmergingTechDto> searchEmergingTechs(
        @P("검색 키워드") String query,
        @P("기술 제공자 필터 (OPENAI, ANTHROPIC, GOOGLE, META, XAI 또는 빈 문자열)") String provider
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: search_emerging_techs(query={}, provider={})", query, provider);

        String queryError = ToolInputValidator.validateRequired(query, "query");
        if (queryError != null) {
            validationErrorCount.incrementAndGet();
            log.warn("Tool 입력값 검증 실패: {}", queryError);
            return List.of();
        }

        String providerError = ToolInputValidator.validateProviderOptional(provider);
        if (providerError != null) {
            validationErrorCount.incrementAndGet();
            log.warn("Tool 입력값 검증 실패: {}", providerError);
            return List.of();
        }

        return emergingTechAdapter.search(query, provider);
    }

    @Tool(name = "create_draft_post",
          value = "Emerging Tech 초안 포스트를 생성합니다. DRAFT 상태로 저장됩니다.")
    public ToolResult createDraftPost(
        @P("포스트 제목") String title,
        @P("포스트 요약 내용") String summary,
        @P("기술 제공자 (OPENAI, ANTHROPIC, GOOGLE, META, XAI)") String provider,
        @P("업데이트 유형 (MODEL_RELEASE, API_UPDATE, SDK_RELEASE, PRODUCT_LAUNCH, PLATFORM_UPDATE, BLOG_POST)") String updateType,
        @P("원본 URL") String url
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: create_draft_post(title={}, provider={}, updateType={})",
            title, provider, updateType);

        // 입력값 검증
        String titleError = ToolInputValidator.validateRequired(title, "title");
        if (titleError != null) { validationErrorCount.incrementAndGet(); return ToolResult.failure(titleError); }

        String summaryError = ToolInputValidator.validateRequired(summary, "summary");
        if (summaryError != null) { validationErrorCount.incrementAndGet(); return ToolResult.failure(summaryError); }

        String providerError = ToolInputValidator.validateProviderRequired(provider);
        if (providerError != null) { validationErrorCount.incrementAndGet(); return ToolResult.failure(providerError); }

        String updateTypeError = ToolInputValidator.validateUpdateType(updateType);
        if (updateTypeError != null) { validationErrorCount.incrementAndGet(); return ToolResult.failure(updateTypeError); }

        String urlError = ToolInputValidator.validateUrl(url);
        if (urlError != null) { validationErrorCount.incrementAndGet(); return ToolResult.failure(urlError); }

        ToolResult result = emergingTechAdapter.createDraft(title, summary, provider, updateType, url);
        if (result.success()) {
            postsCreatedCount.incrementAndGet();
        }
        return result;
    }

    @Tool(name = "publish_post",
          value = "초안 포스트를 게시 상태로 변경합니다.")
    public ToolResult publishPost(
        @P("게시할 포스트 ID") String postId
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: publish_post(postId={})", postId);

        String validationError = ToolInputValidator.validateRequired(postId, "postId");
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(validationError);
        }

        return emergingTechAdapter.publish(postId);
    }

    @Tool(name = "send_slack_notification",
          value = "Slack 채널에 메시지를 전송합니다. 관리자 알림에 사용합니다.")
    public ToolResult sendSlackNotification(
        @P("메시지 내용") String message
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: send_slack_notification(message={})", message);

        String validationError = ToolInputValidator.validateRequired(message, "message");
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(validationError);
        }

        return slackAdapter.sendNotification(message);
    }
}
```

---

## 5. Adapter 클래스 설계

### 5.1 GitHubToolAdapter

```java
@Component
@RequiredArgsConstructor
public class GitHubToolAdapter {

    private final GitHubContract githubApi;

    public List<GitHubReleaseDto> getReleases(String owner, String repo) {
        try {
            List<GitHubDto.Release> releases = githubApi.getReleases(owner, repo, 10);
            return releases.stream()
                .filter(r -> !r.prerelease() && !r.draft())
                .map(r -> new GitHubReleaseDto(
                    r.tagName(),
                    r.name(),
                    truncate(r.body(), 500),
                    r.htmlUrl(),
                    r.publishedAt() != null ? r.publishedAt().toString() : null
                ))
                .toList();
        } catch (Exception e) {
            log.error("GitHub releases 조회 실패: owner={}, repo={}", owner, repo, e);
            return List.of();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
```

### 5.2 ScraperToolAdapter

```java
@Component
@RequiredArgsConstructor
public class ScraperToolAdapter {

    private final RobotsTxtChecker robotsTxtChecker;
    private final WebClient webClient;

    public ScrapedContentDto scrape(String url) {
        try {
            // Robots.txt 확인
            if (!robotsTxtChecker.isAllowed(url, "EmergingTechAgent")) {
                log.warn("Robots.txt에 의해 크롤링 차단: {}", url);
                return new ScrapedContentDto(null, "Robots.txt에 의해 차단됨", url);
            }

            // HTML 가져오기
            String html = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

            // Jsoup으로 파싱
            Document doc = Jsoup.parse(html);
            String title = doc.title();
            String content = doc.select("article, main, .content, .post-content")
                .text();

            return new ScrapedContentDto(
                title,
                truncate(content, 2000),
                url
            );
        } catch (Exception e) {
            log.error("웹 페이지 크롤링 실패: {}", url, e);
            return new ScrapedContentDto(null, "크롤링 실패: " + e.getMessage(), url);
        }
    }
}
```

### 5.3 SlackToolAdapter

```java
@Component
@RequiredArgsConstructor
public class SlackToolAdapter {

    private final SlackContract slackApi;

    @Value("${slack.emerging-tech.channel:#emerging-tech}")
    private String defaultChannel;

    public ToolResult sendNotification(String message) {
        try {
            slackApi.sendInfoNotification(message);
            return ToolResult.success("Slack 알림 전송 완료");
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
            return ToolResult.failure("Slack 알림 전송 실패: " + e.getMessage());
        }
    }
}
```

### 5.4 EmergingTechToolAdapter

```java
@Component
@RequiredArgsConstructor
public class EmergingTechToolAdapter {

    private final EmergingTechInternalContract emergingTechApi;
    private final ObjectMapper objectMapper;

    @Value("${internal-api.emerging-tech.api-key:}")
    private String apiKey;

    private static final String SUCCESS_CODE = "2000";

    @SuppressWarnings("unchecked")
    public List<EmergingTechDto> search(String query, String provider) {
        try {
            String providerParam = (provider != null && !provider.isBlank()) ? provider : null;
            ApiResponse<Object> response = emergingTechApi.searchEmergingTech(apiKey, query, providerParam, 0, 20);

            if (!SUCCESS_CODE.equals(response.code()) || response.data() == null) {
                log.warn("Emerging Tech 검색 실패: code={}, message={}", response.code(), response.message());
                return List.of();
            }

            Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

            if (items == null) {
                return List.of();
            }

            return items.stream()
                .map(item -> new EmergingTechDto(
                    getString(item, "id"),
                    getString(item, "provider"),
                    getString(item, "updateType"),
                    getString(item, "title"),
                    getString(item, "url"),
                    getString(item, "status")
                ))
                .toList();
        } catch (Exception e) {
            log.error("Emerging Tech 검색 실패: query={}, provider={}", query, provider, e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public ToolResult createDraft(String title, String summary,
                                   String provider, String updateType, String url) {
        try {
            InternalApiDto.EmergingTechCreateRequest request = InternalApiDto.EmergingTechCreateRequest.builder()
                .title(title)
                .summary(summary)
                .provider(provider)
                .updateType(updateType)
                .url(url)
                .sourceType("WEB_SCRAPING")
                .status("DRAFT")
                .build();

            ApiResponse<Object> response = emergingTechApi.createEmergingTechInternal(apiKey, request);

            if (SUCCESS_CODE.equals(response.code())) {
                Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);
                String id = getString(data, "id");
                return ToolResult.success("초안 포스트 생성 완료", id);
            } else {
                return ToolResult.failure("포스트 생성 실패: " + response.message());
            }
        } catch (Exception e) {
            log.error("Draft 포스트 생성 실패: title={}", title, e);
            return ToolResult.failure("포스트 생성 실패: " + e.getMessage());
        }
    }

    public ToolResult publish(String postId) {
        try {
            ApiResponse<Object> response = emergingTechApi.approveEmergingTech(apiKey, postId);

            if (SUCCESS_CODE.equals(response.code())) {
                return ToolResult.success("포스트 게시 완료");
            } else {
                return ToolResult.failure("포스트 게시 실패: " + response.message());
            }
        } catch (Exception e) {
            log.error("포스트 게시 실패: postId={}", postId, e);
            return ToolResult.failure("포스트 게시 실패: " + e.getMessage());
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
```

---

## 6. Tool-Service 연결 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                   EmergingTechAgentTools                         │
│  @Tool 어노테이션이 붙은 메서드들                                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┬─────────────┬───────────────┐
        ▼             ▼             ▼             ▼               │
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────────┐   │
│GitHubTool     │ │ScraperTool│ │SlackTool  │ │EmergingTech   │   │
│Adapter        │ │Adapter    │ │Adapter    │ │ToolAdapter    │   │
└───────┬───────┘ └─────┬─────┘ └─────┬─────┘ └───────┬───────┘   │
        │               │             │               │           │
        ▼               ▼             ▼               ▼           │
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────────┐   │
│GitHubContract │ │WebClient+ │ │SlackCont- │ │EmergingTech   │   │
│(client-feign) │ │Jsoup      │ │ract       │ │InternalCont-  │   │
│               │ │(scraper)  │ │(client-   │ │ract           │   │
│               │ │           │ │slack)     │ │(client-feign) │   │
└───────────────┘ └───────────┘ └───────────┘ └───────────────┘   │
```

---

## 7. LangChain4j 설정 클래스

### 7.1 Agent 전용 설정

```java
@Configuration
public class AgentConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o-mini}")
    private String modelName;

    /**
     * Agent용 ChatLanguageModel (OpenAI GPT-4o-mini)
     * Tool 호출을 지원하는 모델
     */
    @Bean("agentChatModel")
    public ChatLanguageModel agentChatLanguageModel() {
        return OpenAiChatModel.builder()
            .apiKey(openAiApiKey)
            .modelName(modelName)
            .temperature(0.3)
            .maxTokens(4096)
            .timeout(Duration.ofSeconds(120))
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
```

### 7.2 설정 파일 (application-agent-api.yml)

```yaml
# Agent용 OpenAI 설정
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY:}
      model-name: gpt-4o-mini
      temperature: 0.3
      max-tokens: 4096
      timeout: 120s

# Emerging Tech 내부 API 설정
internal-api:
  emerging-tech:
    api-key: ${EMERGING_TECH_INTERNAL_API_KEY:}

# Slack 알림
slack:
  emerging-tech:
    channel: "#emerging-tech"
```

---

## 8. Tool 단위 테스트 설계

### 8.1 테스트 클래스

```java
@SpringBootTest
@ActiveProfiles("test")
class EmergingTechAgentToolsTest {

    @Autowired
    private EmergingTechAgentTools tools;

    @MockBean
    private GitHubContract githubApi;

    @MockBean
    private SlackContract slackApi;

    @Test
    void fetchGitHubReleases_shouldReturnReleases() {
        // Given
        when(githubApi.getReleases("openai", "openai-python", 10))
            .thenReturn(List.of(
                new GitHubDto.Release(
                    1L, "v1.0.0", "Release 1.0.0", "Release notes",
                    "https://github.com/...", LocalDateTime.now(),
                    false, false
                )
            ));

        // When
        List<GitHubReleaseDto> releases = tools.fetchGitHubReleases("openai", "openai-python");

        // Then
        assertThat(releases).hasSize(1);
        assertThat(releases.get(0).tagName()).isEqualTo("v1.0.0");
    }

    @Test
    void searchEmergingTechs_shouldReturnEmptyListOnError() {
        // Given - API 호출 실패 시뮬레이션

        // When
        List<EmergingTechDto> results = tools.searchEmergingTechs("GPT-5", "OPENAI");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void createDraftPost_shouldReturnSuccessResult() {
        // Given
        // Mock 설정

        // When
        ToolResult result = tools.createDraftPost(
            "GPT-5 출시", "OpenAI가 GPT-5를 출시했습니다.",
            "OPENAI", "MODEL_RELEASE", "https://openai.com/blog/gpt-5"
        );

        // Then
        assertThat(result.success()).isTrue();
    }

    @Test
    void sendSlackNotification_shouldHandleSuccess() {
        // Given
        doNothing().when(slackApi).sendInfoNotification(anyString());

        // When
        ToolResult result = tools.sendSlackNotification("테스트 알림");

        // Then
        assertThat(result.success()).isTrue();
    }
}
```

---

## 9. 참고 자료

- LangChain4j Tools: https://docs.langchain4j.dev/tutorials/tools
- LangChain4j OpenAI: https://docs.langchain4j.dev/integrations/language-models/open-ai
- @Tool, @P 어노테이션: https://docs.langchain4j.dev/tutorials/tools#tool
- OpenAI API Pricing: https://openai.com/api/pricing/
