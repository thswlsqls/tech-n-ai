# Emerging Tech AI Agent 자율 데이터 수집 기능 설계 프롬프트

## 개요

api/agent 모듈의 AI Agent에 자율 데이터 수집(collect) 기능을 추가한다.
현재 Agent는 읽기 전용 Tool만 보유하고 있으나, 배치 잡이 수행하는 데이터 수집 파이프라인을
Agent 내부에서 직접 실행하여 MongoDB에 저장하고, 수집 결과 통계를 채팅에서 보고할 수 있도록 개선한다.

---

## 1. 현재 구현 검토

### 1.1 Agent 모듈 (api/agent) 현황

**Tools (EmergingTechAgentTools.java)**

| Tool | 역할 | 동작 방식 |
|------|------|-----------|
| `fetch_github_releases` | GitHub 릴리스 조회 (읽기 전용) | GitHubToolAdapter → GitHubContract (Feign) → GitHub API. owner/repo 검증 후 10개 릴리스 조회, draft/prerelease 필터링, body 500자 절단 |
| `scrape_web_page` | 웹 페이지 크롤링 (읽기 전용) | ScraperToolAdapter → RobotsTxtChecker + WebClient + Jsoup. URL 검증 → robots.txt 확인 → HTML 다운로드 → 시맨틱 태그(article, main, .content 등)에서 텍스트 추출 → 2000자 절단 |
| `search_emerging_techs` | 저장된 데이터 검색 (읽기 전용) | EmergingTechToolAdapter → EmergingTechInternalContract (Feign) → api-emerging-tech 모듈 GET 검색. query + provider 파라미터로 최대 20건 조회 |
| `get_emerging_tech_statistics` | 통계 집계 (읽기 전용) | AnalyticsToolAdapter → EmergingTechAggregationService (MongoDB Aggregation) |
| `analyze_text_frequency` | 키워드 빈도 분석 (읽기 전용) | AnalyticsToolAdapter → EmergingTechAggregationService |
| `send_slack_notification` | Slack 알림 전송 | SlackToolAdapter → SlackContract |

**핵심 관찰 사항:**
- 모든 Tool이 읽기 전용이거나 알림 전송용. MongoDB에 데이터를 쓰는 Tool이 없음.
- `EmergingTechInternalContract`에 배치 저장 API(`createEmergingTechBatchInternal`)가 이미 존재하지만, Agent에서 호출하지 않음.
- Agent 모듈은 `client-feign`(GitHub), `client-scraper`를 이미 의존하지만, `client-rss`는 미포함.

### 1.2 배치 잡 3종 현황

**GitHub Job (EmergingTechGitHubJobConfig.java)**
- 대상: OpenAI, Anthropic, Google, Meta 소속 10개 GitHub 저장소
- 파이프라인: GitHubReleasesPagingItemReader → GitHubReleasesProcessor → GitHubReleasesWriter
- 처리: draft/prerelease 필터 → EmergingTechCreateRequest 변환(updateType=SDK_RELEASE, sourceType=GITHUB_RELEASE, externalId="github:{releaseId}") → Internal API 배치 호출

**RSS Job (EmergingTechRssJobConfig.java)**
- 대상: OpenAI Blog RSS, Google AI Blog RSS
- 파이프라인: EmergingTechRssPagingItemReader → EmergingTechRssProcessor → EmergingTechRssWriter
- 처리: 1개월 이내 필터 → 제공자 URL 매핑(openai.com→OPENAI, blog.google→GOOGLE) → 키워드 기반 updateType 분류 → externalId="rss:{SHA256(guid).substring(0,16)}" → Internal API 배치 호출

**Scraper Job (EmergingTechScraperJobConfig.java)**
- 대상: Anthropic News, Meta AI Blog (xAI는 현재 비활성)
- 파이프라인: EmergingTechScrapingItemReader → EmergingTechScraperProcessor → EmergingTechScraperWriter
- 처리: title/url null 필터 → 키워드 기반 updateType 분류 → externalId="scraper:{SHA256(url).substring(0,16)}" → Internal API 배치 호출

### 1.3 Internal API 배치 응답 구조

```java
// InternalApiDto.EmergingTechBatchResponse
{
    totalCount: int,      // 전체 요청 건수
    successCount: int,    // 성공 건수
    newCount: int,        // 신규 저장 건수
    duplicateCount: int,  // 중복 스킵 건수
    failureCount: int,    // 실패 건수
    failureMessages: List<String>  // 실패 사유
}
```

- 중복 체크: `externalId` 우선 조회 → `url` 폴백 조회
- MongoDB `external_id`에 unique index 설정

---

## 2. 개선 요구사항

### 요구사항 1: 자율 데이터 수집
AI Agent가 유저의 자연어 입력에 대하여 배치 잡이 수행하는 데이터 수집과 MongoDB 저장을 자율적으로 수행할 수 있어야 한다.

### 요구사항 2: 수집 결과 보고
수집 및 MongoDB 저장 시도 결과(수집된 데이터 개수, 새롭게 추가된 도큐먼트 개수 등)를 채팅창에 제공할 수 있어야 한다.

---

## 3. 설계 지시사항

다음의 설계 원칙과 아키텍처 청사진에 따라 **구체적인 구현 설계서**를 작성하라.

### 3.1 설계 원칙

- 기존 Adapter 패턴(Tool → Adapter → External Service) 준수
- 기존 client 모듈(client-feign, client-rss, client-scraper)을 재사용하여 배치 잡과 동일한 데이터 수집 수행
- 배치 잡의 Processor 변환 로직을 유틸리티 클래스로 추출하여 Agent에서 공유
- Internal API(`EmergingTechInternalContract.createEmergingTechBatchInternal`)를 통한 MongoDB 저장 (Agent가 MongoDB에 직접 접근하지 않음)
- LangChain4j @Tool 반환값은 JSON으로 자동 직렬화되어 LLM에 전달됨. record/DTO로 통계를 반환하면 LLM이 이를 해석하여 사용자에게 보고
- SOLID 원칙, 클린코드, 최소한의 한글 주석

### 3.2 아키텍처 청사진

#### 신규 파일 목록

| # | 파일 경로 | 역할 |
|---|----------|------|
| 1 | `api/agent/.../tool/dto/DataCollectionResultDto.java` | 수집 결과 통계 DTO (source, totalCollected, totalSent, newCount, duplicateCount, failureCount, failureMessages, summary) |
| 2 | `api/agent/.../tool/adapter/DataCollectionToolAdapter.java` | 수집 파이프라인 오케스트레이터. fetch → process → Internal API 배치 호출 → 결과 통계 반환 |
| 3 | `api/agent/.../tool/util/DataCollectionProcessorUtil.java` | 배치 Processor 변환 로직 추출 유틸리티. GitHub Release/RSS/Scraper → InternalApiDto.EmergingTechCreateRequest 변환. AbstractEmergingTechWriter.java:115-138의 변환 패턴 참조 |
| 4 | `api/agent/.../service/GitHubDataCollectionService.java` | GitHubContract를 사용한 릴리스 조회 + draft/prerelease 필터링 |
| 5 | `api/agent/.../service/RssDataCollectionService.java` | OpenAiBlogRssParser, GoogleAiBlogRssParser 빈을 주입받아 parse() 메서드를 호출하는 얇은 래퍼. 제공자별 필터링 로직만 추가. List\<RssFeedItem\> 반환 |
| 6 | `api/agent/.../service/ScraperDataCollectionService.java` | AnthropicNewsScraper, MetaAiBlogScraper 빈을 주입받아 scrapeArticles() 메서드를 호출하는 얇은 래퍼. 제공자별 필터링 로직만 추가. List\<ScrapedTechArticle\> 반환 |

#### 수정 파일 목록

| # | 파일 경로 | 변경 내용 |
|---|----------|----------|
| 1 | `api/agent/build.gradle` | `implementation project(':client-rss')` 의존성 추가 |
| 2 | `api/agent/.../tool/EmergingTechAgentTools.java` | DataCollectionToolAdapter 주입 + 3개 @Tool 메서드 추가 (collectGitHubReleases, collectRssFeeds, collectScrapedArticles) |
| 3 | `api/agent/.../config/AgentPromptConfig.java` | tools/rules 필드에 3개 수집 Tool 설명 및 워크플로우 규칙 추가 |
| 4 | `api/agent/src/main/resources/application.yml` (또는 application-agent-api.yml) | `client-rss` profile include 추가 (rss 파서가 필요한 설정이 있는 경우) |

#### 데이터 흐름

```
사용자 자연어 입력
    ↓
LLM이 Tool 선택 (예: collect_github_releases)
    ↓
EmergingTechAgentTools.collectGitHubReleases(owner, repo)
    ├─ 입력값 검증 (ToolInputValidator)
    ├─ 메트릭 기록 (metrics.incrementToolCall)
    └─ DataCollectionToolAdapter.collectGitHubReleases(owner, repo)
        ├─ GitHubDataCollectionService.fetchValidReleases(owner, repo, 10)
        │   └─ GitHubContract.getReleases() → draft/prerelease 필터링
        ├─ DataCollectionProcessorUtil.processGitHubRelease(release, owner, repo, provider)
        │   └─ InternalApiDto.EmergingTechCreateRequest 생성
        ├─ EmergingTechInternalContract.createEmergingTechBatchInternal(apiKey, batchRequest)
        │   └─ EmergingTechBatchResponse 수신 (newCount, duplicateCount 등)
        └─ DataCollectionResultDto 구성 및 반환
    ↓
LLM이 DataCollectionResultDto(JSON)를 해석하여 사용자에게 통계 보고
```

### 3.3 Tool 정의 상세

#### collect_github_releases

```java
@Tool(name = "collect_github_releases",
      value = "GitHub 저장소의 릴리스를 수집하여 DB에 저장합니다. "
            + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
public DataCollectionResultDto collectGitHubReleases(
    @P("저장소 소유자 (예: openai, anthropics, google, meta-llama)") String owner,
    @P("저장소 이름 (예: openai-python, anthropic-sdk-python)") String repo
)
```

- 검증: owner/repo 정규식 검증 (기존 ToolInputValidator.validateGitHubRepo 재사용)
- provider 매핑 (EmergingTechGitHubJobConfig.java:51-61 참조):

| Owner | TechProvider |
|-------|-------------|
| openai | OPENAI |
| anthropics | ANTHROPIC |
| google | GOOGLE |
| google-deepmind | GOOGLE |
| meta-llama | META |
| facebookresearch | META |
| xai-org | XAI |

#### collect_rss_feeds

```java
@Tool(name = "collect_rss_feeds",
      value = "OpenAI/Google AI 블로그 RSS 피드를 수집하여 DB에 저장합니다. "
            + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
public DataCollectionResultDto collectRssFeeds(
    @P("제공자 필터 (OPENAI, GOOGLE 또는 빈 문자열=전체 수집)") String provider
)
```

- 검증: provider가 비어있거나 OPENAI/GOOGLE 중 하나인지 확인
- 빈 문자열이면 전체 RSS 소스에서 수집

#### collect_scraped_articles

```java
@Tool(name = "collect_scraped_articles",
      value = "Anthropic/Meta AI 기술 블로그를 크롤링하여 DB에 저장합니다. "
            + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
public DataCollectionResultDto collectScrapedArticles(
    @P("제공자 필터 (ANTHROPIC, META 또는 빈 문자열=전체 수집)") String provider
)
```

- 검증: provider가 비어있거나 ANTHROPIC/META 중 하나인지 확인
- XAI는 현재 비활성 상태이므로 제외 (향후 활성화 시 자동 포함)

### 3.4 DataCollectionResultDto 상세

```java
public record DataCollectionResultDto(
    String source,              // "GITHUB_RELEASES", "RSS_FEEDS", "WEB_SCRAPING"
    String provider,            // 필터된 제공자 (빈 문자열이면 "ALL")
    int totalCollected,         // 외부 소스에서 수집된 원시 데이터 건수 (예: GitHub API에서 15개 릴리스 조회)
    int totalProcessed,         // 유효성 검증 통과 후 Internal API에 전송한 건수 (예: draft 3개 제외 → 12개)
    int newCount,               // DB에 신규 저장된 건수
    int duplicateCount,         // 중복으로 스킵된 건수
    int failureCount,           // 저장 실패 건수
    List<String> failureMessages, // 실패 사유 목록
    String summary              // 사람이 읽을 수 있는 한 줄 요약
) {
    // 팩토리 메서드
    public static DataCollectionResultDto success(...) { ... }
    public static DataCollectionResultDto failure(String source, String provider, int collected, String reason) { ... }
}
```

### 3.5 DataCollectionProcessorUtil 변환 로직

각 배치 Processor의 핵심 로직을 정적 메서드로 추출한다.
Agent 모듈에서는 배치 모듈의 `EmergingTechCreateRequest`(중간 DTO)를 경유하지 않고,
`InternalApiDto.EmergingTechCreateRequest`(Feign DTO)를 직접 생성한다.
이는 불필요한 DTO 변환 단계를 제거하고 모듈 간 의존을 줄이기 위함이다.
(참조: AbstractEmergingTechWriter.java:105-138의 변환 로직)

**GitHub Release 변환** (참조: `GitHubReleasesProcessor.java:56-90`)
```
입력: GitHubDto.Release, owner, repo, TechProvider
출력: InternalApiDto.EmergingTechCreateRequest
변환:
  - provider: TechProvider.name()
  - updateType: SDK_RELEASE
  - title: release.name() ?? release.tagName()
  - summary: truncate(release.body(), 500)
  - url: release.htmlUrl()
  - publishedAt: ISO_DATE_TIME 파싱 (실패 시 now())
  - sourceType: GITHUB_RELEASE
  - status: DRAFT
  - externalId: "github:" + release.id()
  - metadata: version=tagName, tags=[sdk,release], author=release.author.login, githubRepo=owner/repo
```

**RSS Feed 변환** (참조: `EmergingTechRssProcessor.java:42-104`)
```
입력: RssFeedItem
출력: InternalApiDto.EmergingTechCreateRequest
변환:
  - provider: URL 기반 매핑 (openai.com→OPENAI, blog.google→GOOGLE)
  - updateType: 키워드 분류 (api→API_UPDATE, release/introducing/model→MODEL_RELEASE, ...)
  - title: item.title()
  - summary: HTML 태그 제거 + truncate(500)
  - url: item.link()
  - publishedAt: item.publishedDate() (1개월 이내 필터)
  - sourceType: RSS
  - status: DRAFT
  - externalId: "rss:" + SHA256(item.guid() ?? item.link()).substring(0,16)
  - metadata: tags=item.categories()
```

**Scraped Article 변환** (참조: `EmergingTechScraperProcessor.java:28-82`)
```
입력: ScrapedTechArticle
출력: InternalApiDto.EmergingTechCreateRequest
변환:
  - provider: article.providerName() → TechProvider 매핑
  - updateType: 키워드 분류 (RSS와 동일 로직)
  - title: article.title()
  - summary: truncate(article.summary(), 500)  // ScrapedTechArticle의 필드명은 summary (description 아님)
  - url: article.url()
  - publishedAt: article.publishedAt()
  - sourceType: WEB_SCRAPING
  - status: DRAFT
  - externalId: "scraper:" + SHA256(article.url()).substring(0,16)
  - metadata: tags=article.categories()
```

### 3.6 AgentPromptConfig 변경

**tools 필드 추가:**
```
- collect_github_releases: GitHub 저장소 릴리스 수집 및 DB 저장 (결과 통계 포함)
- collect_rss_feeds: OpenAI/Google 블로그 RSS 피드 수집 및 DB 저장 (결과 통계 포함)
- collect_scraped_articles: Anthropic/Meta 블로그 크롤링 및 DB 저장 (결과 통계 포함)
```

**rules 필드 추가:**
```
7. 데이터 수집 요청 시 collect_* 도구를 사용하여 자동으로 DB에 저장하고, 결과 통계를 보고
8. 전체 소스 수집 요청 시: collect_github_releases(각 저장소별) → collect_rss_feeds("") → collect_scraped_articles("") 순서로 실행
9. 수집 결과의 신규/중복/실패 건수를 Markdown 표로 정리하여 제공
```

### 3.7 에러 처리 전략

| 계층 | 에러 유형 | 처리 방식 |
|------|----------|----------|
| Service | 외부 API 호출 실패 (FeignException, RssParsingException, ScrapingException 등) | 예외 catch → 빈 리스트 반환 → 다른 소스 계속 진행 |
| ProcessorUtil | 변환 실패 | null 반환 → 해당 아이템 스킵 → 로그 기록 |
| Adapter | Internal API 호출 실패 | FeignException catch → DataCollectionResultDto.failure() 반환 |
| Tool | 입력값 검증 실패 | DataCollectionResultDto.failure() 즉시 반환 |

### 3.8 build.gradle 변경

```gradle
// 기존
implementation project(':client-feign')
implementation project(':client-slack')
implementation project(':client-scraper')

// 추가
implementation project(':client-rss')
```

---

## 4. 참고 자료

### 공식 문서
- [LangChain4j Tools (Function Calling)](https://docs.langchain4j.dev/tutorials/tools/) - @Tool 반환 타입, @P 파라미터 설명, 에러 처리
- [LangChain4j AI Services](https://docs.langchain4j.dev/tutorials/ai-services/) - AiServices 프록시 동작 원리, 구조화된 출력
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j) - 소스코드 및 예제

### LangChain4j @Tool 베스트 프랙티스
1. **명확한 설명**: Tool의 name, value(설명), 각 파라미터의 @P 설명을 최대한 구체적으로 작성
2. **반환 타입**: void → "Success" 문자열, String → 그대로, 기타 타입 → JSON 직렬화 후 LLM에 전달
3. **에러 처리**: 예외 발생 시 e.getMessage()가 LLM에 전달됨. `toolExecutionErrorHandler`로 커스터마이즈 가능
4. **입력 검증**: LLM hallucination 방어를 위해 모든 입력값을 사전 검증

### 핵심 참조 파일
- `api/agent/.../tool/EmergingTechAgentTools.java` - 현재 Tool 정의
- `api/agent/.../tool/adapter/GitHubToolAdapter.java` - Adapter 패턴 참조
- `api/agent/.../config/AgentPromptConfig.java` - 시스템 프롬프트 구성
- `batch/source/.../github/processor/GitHubReleasesProcessor.java` - GitHub 변환 로직
- `batch/source/.../rss/processor/EmergingTechRssProcessor.java` - RSS 변환 로직
- `batch/source/.../scraper/processor/EmergingTechScraperProcessor.java` - Scraper 변환 로직
- `batch/source/.../writer/AbstractEmergingTechWriter.java` - Internal API 호출 패턴
- `client/feign/.../internal/contract/InternalApiDto.java` - 배치 요청/응답 DTO
- `client/feign/.../internal/contract/EmergingTechInternalContract.java` - Internal API 계약

---

## 5. 구현 순서

1. DTO/유틸리티 생성: DataCollectionResultDto, DataCollectionProcessorUtil
2. 서비스 생성: GitHubDataCollectionService, RssDataCollectionService, ScraperDataCollectionService
3. Adapter 생성: DataCollectionToolAdapter
4. Tool 통합: EmergingTechAgentTools에 3개 @Tool 메서드 추가
5. 설정 변경: build.gradle, AgentPromptConfig, application.yml
6. 검증: 빌드 확인 및 통합 테스트
