# Phase 2: LangChain4j Tool 래퍼 설계서

## 1. 개요

### 1.1 목적
Phase 1에서 구축한 데이터 수집 파이프라인의 기능들을 LangChain4j Tool로 래핑하여 AI Agent가 호출할 수 있도록 한다.

### 1.2 전제 조건
- Phase 1 완료: AiUpdate 엔티티, API 엔드포인트, Batch Job 구축
- 기존 `api-chatbot` 모듈에 langchain4j 0.35.0 의존성 존재

---

## 2. 의존성 설정

### 2.1 기존 의존성 (api-chatbot/build.gradle)
```gradle
// 이미 존재
implementation 'dev.langchain4j:langchain4j:0.35.0'
implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0'
```

### 2.2 추가 의존성
```gradle
// OpenAI 모델 지원 (Agent용) - 기존 의존성 활용
// implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0' (이미 존재)

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
| `search_ai_updates` | 저장된 AI 업데이트 검색 | AiUpdateService |
| `create_draft_post` | Draft 포스트 생성 | AiUpdateInternalContract |
| `publish_post` | 포스트 게시 (승인) | AiUpdateInternalContract |
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

// AI Update DTO (Tool 응답용)
public record AiUpdateDto(
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
api/chatbot/src/main/java/.../chatbot/
├── tool/
│   ├── AiUpdateAgentTools.java      // Tool 메서드 정의
│   ├── dto/
│   │   ├── ToolResult.java
│   │   ├── GitHubReleaseDto.java
│   │   ├── AiUpdateDto.java
│   │   └── ScrapedContentDto.java
│   └── adapter/
│       ├── GitHubToolAdapter.java   // GitHubContract 래핑
│       ├── ScraperToolAdapter.java  // WebScraper 래핑
│       ├── SlackToolAdapter.java    // SlackContract 래핑
│       └── AiUpdateToolAdapter.java // AiUpdateService 래핑
```

### 4.2 Tool 클래스 구현

```java
package com.tech.n.ai.api.chatbot.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiUpdateAgentTools {

    private final GitHubToolAdapter githubAdapter;
    private final ScraperToolAdapter scraperAdapter;
    private final SlackToolAdapter slackAdapter;
    private final AiUpdateToolAdapter aiUpdateAdapter;

    @Tool("GitHub 저장소의 최신 릴리스 목록을 가져옵니다. SDK 업데이트 확인에 사용합니다.")
    public List<GitHubReleaseDto> fetchGitHubReleases(
        @P("저장소 소유자 (예: openai, anthropics)") String owner,
        @P("저장소 이름 (예: openai-python, anthropic-sdk-python)") String repo
    ) {
        log.info("Tool 호출: fetchGitHubReleases(owner={}, repo={})", owner, repo);
        return githubAdapter.getReleases(owner, repo);
    }

    @Tool("웹 페이지를 크롤링하여 텍스트 내용을 추출합니다. 블로그 포스트 내용 확인에 사용합니다.")
    public ScrapedContentDto scrapeWebPage(
        @P("크롤링할 웹 페이지 URL") String url
    ) {
        log.info("Tool 호출: scrapeWebPage(url={})", url);
        return scraperAdapter.scrape(url);
    }

    @Tool("저장된 AI 업데이트를 검색합니다. 중복 확인이나 기존 데이터 조회에 사용합니다.")
    public List<AiUpdateDto> searchAiUpdates(
        @P("검색 키워드") String query,
        @P("AI 제공자 필터 (OPENAI, ANTHROPIC, GOOGLE, META 또는 빈 문자열)") String provider
    ) {
        log.info("Tool 호출: searchAiUpdates(query={}, provider={})", query, provider);
        return aiUpdateAdapter.search(query, provider);
    }

    @Tool("AI 업데이트 초안 포스트를 생성합니다. DRAFT 상태로 저장됩니다.")
    public ToolResult createDraftPost(
        @P("포스트 제목") String title,
        @P("포스트 요약 내용") String summary,
        @P("AI 제공자 (OPENAI, ANTHROPIC, GOOGLE, META)") String provider,
        @P("업데이트 유형 (MODEL_RELEASE, API_UPDATE, SDK_RELEASE, BLOG_POST)") String updateType,
        @P("원본 URL") String url
    ) {
        log.info("Tool 호출: createDraftPost(title={}, provider={}, updateType={})",
            title, provider, updateType);
        return aiUpdateAdapter.createDraft(title, summary, provider, updateType, url);
    }

    @Tool("초안 포스트를 게시 상태로 변경합니다.")
    public ToolResult publishPost(
        @P("게시할 포스트 ID") String postId
    ) {
        log.info("Tool 호출: publishPost(postId={})", postId);
        return aiUpdateAdapter.publish(postId);
    }

    @Tool("Slack 채널에 메시지를 전송합니다. 관리자 알림에 사용합니다.")
    public ToolResult sendSlackNotification(
        @P("메시지 내용") String message
    ) {
        log.info("Tool 호출: sendSlackNotification(message={})", message);
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
            if (!robotsTxtChecker.isAllowed(url, "AiUpdateAgent")) {
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

    @Value("${slack.ai-update.channel:#ai-updates}")
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

### 5.4 AiUpdateToolAdapter

```java
@Component
@RequiredArgsConstructor
public class AiUpdateToolAdapter {

    private final AiUpdateInternalContract aiUpdateApi;

    @Value("${internal-api.ai-update.api-key}")
    private String apiKey;

    public List<AiUpdateDto> search(String query, String provider) {
        try {
            AiUpdateSearchRequest request = AiUpdateSearchRequest.builder()
                .query(query)
                .provider(provider.isBlank() ? null : AiProvider.valueOf(provider))
                .size(20)
                .build();

            ApiResponse<AiUpdateSearchResponse> response = aiUpdateApi.search(apiKey, request);

            if (!"2000".equals(response.code())) {
                return List.of();
            }

            return response.data().items().stream()
                .map(item -> new AiUpdateDto(
                    item.id(),
                    item.provider(),
                    item.updateType(),
                    item.title(),
                    item.url(),
                    item.status()
                ))
                .toList();
        } catch (Exception e) {
            log.error("AI Update 검색 실패", e);
            return List.of();
        }
    }

    public ToolResult createDraft(String title, String summary,
                                   String provider, String updateType, String url) {
        try {
            AiUpdateCreateRequest request = AiUpdateCreateRequest.builder()
                .title(title)
                .summary(summary)
                .provider(AiProvider.valueOf(provider))
                .updateType(AiUpdateType.valueOf(updateType))
                .url(url)
                .sourceType(SourceType.WEB_SCRAPING)
                .status(PostStatus.DRAFT)
                .build();

            ApiResponse<AiUpdateDetailResponse> response =
                aiUpdateApi.createInternal(apiKey, request);

            if ("2000".equals(response.code())) {
                return ToolResult.success(
                    "초안 포스트 생성 완료",
                    response.data().id()
                );
            } else {
                return ToolResult.failure("포스트 생성 실패: " + response.message());
            }
        } catch (Exception e) {
            log.error("Draft 포스트 생성 실패", e);
            return ToolResult.failure("포스트 생성 실패: " + e.getMessage());
        }
    }

    public ToolResult publish(String postId) {
        try {
            ApiResponse<AiUpdateDetailResponse> response =
                aiUpdateApi.approve(apiKey, postId);

            if ("2000".equals(response.code())) {
                return ToolResult.success("포스트 게시 완료");
            } else {
                return ToolResult.failure("포스트 게시 실패: " + response.message());
            }
        } catch (Exception e) {
            log.error("포스트 게시 실패: postId={}", postId, e);
            return ToolResult.failure("포스트 게시 실패: " + e.getMessage());
        }
    }
}
```

---

## 6. Tool-Service 연결 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                      AiUpdateAgentTools                         │
│  @Tool 어노테이션이 붙은 메서드들                                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┬─────────────┬───────────────┐
        ▼             ▼             ▼             ▼               │
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────────┐   │
│GitHubTool     │ │ScraperTool│ │SlackTool  │ │AiUpdateTool   │   │
│Adapter        │ │Adapter    │ │Adapter    │ │Adapter        │   │
└───────┬───────┘ └─────┬─────┘ └─────┬─────┘ └───────┬───────┘   │
        │               │             │               │           │
        ▼               ▼             ▼               ▼           │
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────────┐   │
│GitHubContract │ │WebClient+ │ │SlackCont- │ │AiUpdateInter- │   │
│(client-feign) │ │Jsoup      │ │ract       │ │nalContract    │   │
│               │ │(scraper)  │ │(client-   │ │(client-feign) │   │
│               │ │           │ │slack)     │ │               │   │
└───────────────┘ └───────────┘ └───────────┘ └───────────────┘   │
```

---

## 7. LangChain4j 설정 클래스

### 7.1 Agent 전용 설정

```java
@Configuration
public class AiAgentConfig {

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
            .apiKey(openAiApiKey)  // 기존 OpenAI API 키 재사용
            .modelName(modelName)
            .temperature(0.3)  // Tool 호출에는 낮은 temperature
            .maxTokens(4096)
            .timeout(Duration.ofSeconds(120))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    /**
     * AiUpdateAgentTools Bean
     */
    @Bean
    public AiUpdateAgentTools aiUpdateAgentTools(
            GitHubToolAdapter githubAdapter,
            ScraperToolAdapter scraperAdapter,
            SlackToolAdapter slackAdapter,
            AiUpdateToolAdapter aiUpdateAdapter) {
        return new AiUpdateAgentTools(
            githubAdapter, scraperAdapter, slackAdapter, aiUpdateAdapter
        );
    }
}
```

### 7.2 설정 파일 (application.yml)

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}  # 기존 API 키 재사용
      model-name: gpt-4o-mini     # Agent용 모델
    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small  # 기존 유지

internal-api:
  ai-update:
    api-key: ${AI_UPDATE_INTERNAL_API_KEY}

slack:
  ai-update:
    channel: "#ai-updates"
```

---

## 8. Tool 단위 테스트 설계

### 8.1 테스트 클래스

```java
@SpringBootTest
@ActiveProfiles("test")
class AiUpdateAgentToolsTest {

    @Autowired
    private AiUpdateAgentTools tools;

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
    void searchAiUpdates_shouldReturnEmptyListOnError() {
        // Given - API 호출 실패 시뮬레이션

        // When
        List<AiUpdateDto> results = tools.searchAiUpdates("GPT-5", "OPENAI");

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
