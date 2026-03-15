# Phase 2: LangChain4j Tool 래퍼 설계서 작성 프롬프트

## 목표
Phase 1에서 구축한 데이터 수집 파이프라인을 LangChain4j Tool로 래핑하여 AI Agent가 호출할 수 있도록 설계한다.

## 전제 조건
- Phase 1 완료: 데이터 수집 파이프라인 및 API 엔드포인트 구축됨
- 기존 `api-chatbot` 모듈에 langchain4j 의존성 존재

## 설계서에 포함할 내용

### 1. LangChain4j 의존성 확인/추가
```gradle
// api-chatbot/build.gradle 또는 신규 모듈
implementation 'dev.langchain4j:langchain4j:0.36.2'
implementation 'dev.langchain4j:langchain4j-anthropic:0.36.2'
```

공식 문서: https://docs.langchain4j.dev/

### 2. Tool 인터페이스 설계
Agent가 사용할 Tool 목록:

| Tool Name | 설명 | 입력 | 출력 |
|-----------|------|------|------|
| `fetch_rss_feed` | RSS 피드에서 기사 목록 조회 | url: String | List<Article> |
| `fetch_github_releases` | GitHub 릴리스 조회 | owner: String, repo: String | List<Release> |
| `scrape_web_page` | 웹 페이지 크롤링 | url: String | String (content) |
| `search_ai_updates` | 기존 업데이트 검색 | query: String, provider: String | List<AiUpdate> |
| `create_draft_post` | Draft 포스트 생성 | title, content, provider, updateType | Post |
| `publish_post` | 포스트 게시 | postId: String | Result |
| `send_slack_notification` | Slack 알림 발송 | channel, message | Result |

### 3. Tool 클래스 구현 설계
```java
@Component
public class AiUpdateTools {

    private final RssClient rssClient;
    private final GitHubContract githubApi;
    private final ScraperService scraperService;
    private final AiUpdateService aiUpdateService;
    private final SlackService slackService;

    @Tool("RSS 피드 URL에서 최신 기사 목록을 가져옵니다")
    public List<ArticleDto> fetchRssFeed(
        @P("RSS 피드 URL") String url
    ) {
        return rssClient.fetch(url);
    }

    @Tool("GitHub 저장소의 최신 릴리스 목록을 가져옵니다")
    public List<ReleaseDto> fetchGitHubReleases(
        @P("저장소 소유자") String owner,
        @P("저장소 이름") String repo
    ) {
        return githubApi.getReleases(owner, repo);
    }

    @Tool("웹 페이지를 크롤링하여 텍스트 내용을 추출합니다")
    public String scrapeWebPage(
        @P("크롤링할 URL") String url
    ) {
        return scraperService.scrape(url);
    }

    @Tool("저장된 AI 업데이트를 검색합니다")
    public List<AiUpdateDto> searchAiUpdates(
        @P("검색 키워드") String query,
        @P("AI 제공자 (OPENAI, ANTHROPIC, GOOGLE, META)") String provider
    ) {
        return aiUpdateService.search(query, provider);
    }

    @Tool("AI 업데이트 초안 포스트를 생성합니다")
    public PostDto createDraftPost(
        @P("포스트 제목") String title,
        @P("포스트 본문") String content,
        @P("AI 제공자") String provider,
        @P("업데이트 유형") String updateType
    ) {
        return aiUpdateService.createDraft(title, content, provider, updateType);
    }

    @Tool("초안 포스트를 게시 상태로 변경합니다")
    public ResultDto publishPost(
        @P("포스트 ID") String postId
    ) {
        return aiUpdateService.publish(postId);
    }

    @Tool("Slack 채널에 메시지를 전송합니다")
    public ResultDto sendSlackNotification(
        @P("Slack 채널") String channel,
        @P("메시지 내용") String message
    ) {
        return slackService.send(channel, message);
    }
}
```

### 4. 기존 서비스와 Tool 연결
Phase 1에서 구현한 서비스들을 Tool에서 호출하는 어댑터 패턴:

```
Tool Method → Adapter → Existing Service/Client
     ↓
fetchRssFeed() → RssClient (client-rss)
fetchGitHubReleases() → GitHubContract (client-feign)
scrapeWebPage() → ScraperService (client-scraper)
createDraftPost() → AiUpdateService (api-ai-update 또는 api-news)
sendSlackNotification() → SlackService (client-slack)
```

### 5. Tool 테스트 설계
각 Tool의 단위 테스트:
```java
@Test
void fetchRssFeed_shouldReturnArticles() {
    // Given
    String url = "https://openai.com/blog/rss";

    // When
    List<ArticleDto> articles = aiUpdateTools.fetchRssFeed(url);

    // Then
    assertThat(articles).isNotEmpty();
}
```

### 6. Tool 등록 및 설정
```java
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel claudeModel() {
        return AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName("claude-sonnet-4-20250514")
            .build();
    }

    @Bean
    public AiUpdateTools aiUpdateTools(...) {
        return new AiUpdateTools(...);
    }
}
```

## 제약 조건
- Tool 메서드는 단순하게 유지 (복잡한 로직은 서비스 계층에 위임)
- @Tool 어노테이션의 description은 LLM이 이해하기 쉽게 작성
- @P 어노테이션으로 파라미터 설명 명확히 작성
- 오버엔지니어링 금지: 현재 필요한 Tool만 구현

## 산출물
1. Tool 클래스 설계 (메서드 시그니처, 어노테이션)
2. Tool-Service 연결 다이어그램
3. LangChain4j 설정 클래스
4. Tool 단위 테스트 명세

## 참고 자료
- LangChain4j Tools: https://docs.langchain4j.dev/tutorials/tools
- LangChain4j Anthropic: https://docs.langchain4j.dev/integrations/language-models/anthropic
