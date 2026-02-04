# Phase 1: 데이터 수집 파이프라인 설계서

## 1. 개요

### 1.1 목적
빅테크 IT 기업(OpenAI, Anthropic, Google, Meta, xAI 등)의 기술 뉴스, 제품 릴리즈, 업데이트 정보를 종합적으로 자동 수집하는 Spring Batch 기반 파이프라인 구축. AI 관련 뉴스뿐만 아니라 IT 전반의 최신 기술 동향을 포괄적으로 수집

### 1.2 범위
- 공식 블로그/뉴스 RSS 수집 (OpenAI, Google)
- 웹 페이지 크롤링 - RSS 미제공 소스 (Anthropic, Meta, xAI)
- GitHub Releases API 연동
- Draft 상태 포스트 저장 및 Slack 알림

### 1.3 수집 방식 선정 근거

| Provider | RSS 제공 여부 | 페이지 렌더링 방식 | 선정 방식 | 사유 |
|----------|-------------|-------------------|----------|------|
| OpenAI | `/blog/rss.xml` 제공 | Cloudflare 403 차단 | **RSS** (client-rss) | HTML 크롤링 불가, RSS 안정적 |
| Google AI | `/technology/ai/rss/` 제공 | JS SPA | **RSS** (client-rss) | SPA 렌더링 불필요, RSS 안정적 |
| Anthropic | 미제공 | Next.js SSR | **HTML 크롤링** (client-scraper) | RSS 없음, SSR이므로 Jsoup 가능 |
| Meta AI | 미제공 | React SPA | **HTML 크롤링** (client-scraper) | RSS 없음, SPA이므로 Selenium 필요 가능성 |
| xAI (Grok) | 미제공 | Next.js SSR + Cloudflare | **HTML 크롤링** (client-scraper) | RSS 없음, Cloudflare 403 차단으로 Selenium 필수 |

---

## 2. 데이터 소스 정의

### 2.1 소스별 수집 방식

| Provider | 소스 | URL | 수집 방식 | 클라이언트 |
|----------|------|-----|----------|-----------|
| OpenAI | Blog (RSS) | https://openai.com/blog/rss.xml | RSS 2.0 파싱 | client-rss |
| OpenAI | GitHub | https://github.com/openai/openai-python | GitHub API | client-feign |
| Anthropic | News | https://www.anthropic.com/news | HTML 크롤링 (SSR) | client-scraper |
| Anthropic | GitHub | https://github.com/anthropics/anthropic-sdk-python | GitHub API | client-feign |
| Google | AI Blog (RSS) | https://blog.google/technology/ai/rss/ | RSS 2.0 파싱 | client-rss |
| Meta | AI Blog | https://ai.meta.com/blog/ | HTML 크롤링 (SPA) | client-scraper |
| Meta | GitHub | https://github.com/facebookresearch/llama | GitHub API | client-feign |
| xAI | News | https://x.ai/news | HTML 크롤링 (SSR + Cloudflare) | client-scraper |
| xAI | GitHub | https://github.com/xai-org/grok-1 | GitHub API | client-feign |

### 2.2 GitHub 대상 저장소

```yaml
emerging-tech:
  github:
    repositories:
      - owner: openai
        repo: openai-python
      - owner: anthropics
        repo: anthropic-sdk-python
      - owner: google
        repo: generative-ai-python
      - owner: facebookresearch
        repo: llama
      - owner: xai-org
        repo: grok-1
```

---

## 3. 도큐먼트 스키마 (MongoDB)

> **저장소 전략**: EmergingTech 수집 데이터는 MongoDB에만 저장하고 조회합니다.
> MariaDB(Aurora)는 사용하지 않으며, CQRS 패턴 없이 MongoDB 단일 저장소로 운영합니다.

### 3.1 Enum 정의

```java
public enum TechProvider {
    OPENAI, ANTHROPIC, GOOGLE, META, XAI
    // 향후 확장: MICROSOFT, APPLE, AMAZON, NVIDIA 등 IT 기업 추가 가능
}

public enum EmergingTechType {
    MODEL_RELEASE,      // AI 모델 출시 (GPT-5, Claude 4 등)
    API_UPDATE,         // API 변경사항
    SDK_RELEASE,        // SDK 새 버전
    PRODUCT_LAUNCH,     // 신규 제품/서비스 출시
    PLATFORM_UPDATE,    // 플랫폼 업데이트 (클라우드, 인프라 등)
    BLOG_POST           // 일반 기술 블로그 포스트
}

public enum SourceType {
    RSS,
    GITHUB_RELEASE,
    WEB_SCRAPING
}

public enum PostStatus {
    DRAFT,      // 초안 (자동 수집됨)
    PENDING,    // 승인 대기
    PUBLISHED,  // 게시됨
    REJECTED    // 거부됨
}
```

### 3.2 EmergingTechDocument (MongoDB)

```java
@Document(collection = "emerging_techs")
@Getter
@Setter
public class EmergingTechDocument {

    @Id
    private ObjectId id;

    @Field("provider")
    @Indexed
    private String provider;

    @Field("update_type")
    private String updateType;

    @Field("title")
    private String title;

    @Field("summary")
    private String summary;

    @Field("url")
    @Indexed
    private String url;

    @Field("published_at")
    private LocalDateTime publishedAt;

    @Field("source_type")
    private String sourceType;

    @Field("status")
    @Indexed
    private String status;

    @Field("metadata")
    private EmergingTechMetadata metadata;

    @Field("external_id")
    @Indexed(unique = true)
    private String externalId;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    public static class EmergingTechMetadata {
        @Field("version")
        private String version;  // SDK 버전

        @Field("tags")
        private List<String> tags;

        @Field("author")
        private String author;

        @Field("github_repo")
        private String githubRepo;
    }
}
```

---

## 4. Batch Job 구조

### 4.1 디렉토리 구조

```
batch/source/domain/emergingtech/
├── dto/
│   └── request/
│       └── EmergingTechCreateRequest.java
├── github/
│   ├── jobconfig/EmergingTechGitHubJobConfig.java
│   ├── jobparameter/EmergingTechGitHubJobParameter.java
│   ├── incrementer/EmergingTechGitHubIncrementer.java
│   ├── listener/EmergingTechGitHubJobListener.java
│   ├── reader/GitHubReleasesPagingItemReader.java
│   ├── processor/GitHubReleasesProcessor.java
│   ├── writer/GitHubReleasesWriter.java
│   └── service/GitHubReleasesService.java
├── rss/
│   ├── jobconfig/EmergingTechRssJobConfig.java
│   ├── jobparameter/EmergingTechRssJobParameter.java
│   ├── incrementer/EmergingTechRssIncrementer.java
│   ├── listener/EmergingTechRssJobListener.java
│   ├── reader/EmergingTechRssPagingItemReader.java
│   ├── processor/EmergingTechRssProcessor.java
│   ├── writer/EmergingTechRssWriter.java
│   └── service/EmergingTechRssService.java
└── scraper/
    ├── jobconfig/EmergingTechScraperJobConfig.java
    ├── jobparameter/EmergingTechScraperJobParameter.java
    ├── incrementer/EmergingTechScraperIncrementer.java
    ├── listener/EmergingTechScraperJobListener.java
    ├── reader/EmergingTechScrapingItemReader.java
    ├── processor/EmergingTechScraperProcessor.java
    ├── writer/EmergingTechScraperWriter.java
    └── service/EmergingTechScraperService.java
```

### 4.2 Job 설계 - GitHub Releases

```java
@Configuration
@RequiredArgsConstructor
public class EmergingTechGitHubJobConfig {

    private static final String JOB_NAME = Constants.EMERGING_TECH_GITHUB;
    private static final String STEP1_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final GitHubReleasesService gitHubReleasesService;
    private final EmergingTechInternalContract emergingTechInternalApi;

    private static final List<GitHubReleasesPagingItemReader.RepositoryInfo> TARGET_REPOSITORIES = List.of(
        new GitHubReleasesPagingItemReader.RepositoryInfo("openai", "openai-python", TechProvider.OPENAI.name()),
        new GitHubReleasesPagingItemReader.RepositoryInfo("anthropics", "anthropic-sdk-python", TechProvider.ANTHROPIC.name()),
        new GitHubReleasesPagingItemReader.RepositoryInfo("google", "generative-ai-python", TechProvider.GOOGLE.name()),
        new GitHubReleasesPagingItemReader.RepositoryInfo("facebookresearch", "llama", TechProvider.META.name()),
        new GitHubReleasesPagingItemReader.RepositoryInfo("xai-org", "grok-1", TechProvider.XAI.name())
    );

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository,
                   @Qualifier(STEP1_NAME) Step step1,
                   @Qualifier(JOB_NAME + ".listener") EmergingTechGitHubJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step1)
            .incrementer(new EmergingTechGitHubIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = STEP1_NAME)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP1_NAME + Constants.ITEM_READER) GitHubReleasesPagingItemReader reader,
                      @Qualifier(STEP1_NAME + Constants.ITEM_PROCESSOR) GitHubReleasesProcessor processor,
                      @Qualifier(STEP1_NAME + Constants.ITEM_WRITER) GitHubReleasesWriter writer) {
        return new StepBuilder(STEP1_NAME, jobRepository)
            .<GitHubReleaseWithRepo, EmergingTechCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
```

### 4.3 Reader 설계 - GitHub Releases

```java
public class GitHubReleasesPagingItemReader extends AbstractPagingItemReader<GitHubReleaseWithRepo> {

    private final GitHubReleasesService gitHubReleasesService;
    private final List<RepositoryInfo> repositories;
    private List<GitHubReleaseWithRepo> allItems;

    public record RepositoryInfo(String owner, String repo, String providerName) {}

    @Override
    protected void doReadPage() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            for (RepositoryInfo repoInfo : repositories) {
                List<GitHubReleaseWithRepo> releases = gitHubReleasesService.getReleases(repoInfo);
                allItems.addAll(releases);
            }
        }

        int start = getPage() * getPageSize();
        int end = Math.min(start + getPageSize(), allItems.size());

        if (start >= allItems.size()) {
            results = Collections.emptyList();
            return;
        }

        results = new ArrayList<>(allItems.subList(start, end));
    }
}
```

### 4.4 Processor 설계 - GitHub Releases

```java
public class GitHubReleasesProcessor implements ItemProcessor<GitHubReleaseWithRepo, EmergingTechCreateRequest> {

    @Override
    public EmergingTechCreateRequest process(GitHubReleaseWithRepo item) {
        GitHubDto.Release release = item.release();

        // prerelease, draft 제외
        if (Boolean.TRUE.equals(release.prerelease()) || Boolean.TRUE.equals(release.draft())) {
            return null;
        }

        return EmergingTechCreateRequest.builder()
            .provider(TechProvider.valueOf(item.providerName()))
            .updateType(EmergingTechType.SDK_RELEASE)
            .title(release.name() != null ? release.name() : release.tagName())
            .summary(truncate(release.body(), 500))
            .url(release.htmlUrl())
            .publishedAt(release.publishedAt())
            .sourceType(SourceType.GITHUB_RELEASE)
            .status(PostStatus.DRAFT)
            .externalId("github:" + release.id())
            .metadata(Map.of(
                "version", release.tagName(),
                "github_repo", item.owner() + "/" + item.repo()
            ))
            .build();
    }
}
```

### 4.5 Writer 설계

```java
public class GitHubReleasesWriter implements ItemWriter<EmergingTechCreateRequest> {

    private final EmergingTechInternalContract emergingTechApi;

    @Override
    public void write(Chunk<? extends EmergingTechCreateRequest> chunk) {
        List<EmergingTechCreateRequest> items = new ArrayList<>(chunk.getItems());
        if (items.isEmpty()) return;

        for (EmergingTechCreateRequest item : items) {
            emergingTechApi.createInternal(item);
        }
    }
}
```

---

## 5. RSS 수집 설계 (OpenAI, Google AI)

### 5.1 대상 RSS 피드

| Provider | RSS URL | 피드 형식 | 주요 필드 |
|----------|---------|----------|----------|
| OpenAI | `https://openai.com/blog/rss.xml` | RSS 2.0 | title, description, link, pubDate, category, guid |
| Google AI | `https://blog.google/technology/ai/rss/` | RSS 2.0 | title, link, pubDate, description, category, media:content, author |

### 5.2 client-rss 모듈 확장

#### 5.2.1 새로운 RSS Parser 구현

기존 `RssParser` 인터페이스를 구현하여 기술 블로그 전용 파서 추가:

```
client/rss/src/main/java/com/ebson/shrimp/tm/demo/client/rss/parser/
├── RssParser.java                      # 기존 인터페이스
├── TechCrunchRssParser.java            # 기존 구현
├── OpenAiBlogRssParser.java            # 신규 - OpenAI Blog
└── GoogleAiBlogRssParser.java          # 신규 - Google AI Blog
```

#### 5.2.2 OpenAiBlogRssParser

```java
@Component
@Slf4j
public class OpenAiBlogRssParser implements RssParser {

    private final WebClient.Builder webClientBuilder;
    private final RssProperties properties;
    private final RetryRegistry retryRegistry;

    public OpenAiBlogRssParser(
            @Qualifier("rssWebClientBuilder") WebClient.Builder webClientBuilder,
            RssProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("openai-blog");
        if (config == null) {
            throw new RssParsingException("OpenAI Blog RSS source configuration not found");
        }

        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        Retry retry = retryRegistry.retry("rssRetry");

        return retry.executeSupplier(() -> {
            try {
                log.debug("Fetching OpenAI Blog RSS feed from: {}", config.getFeedUrl());
                String feedContent = webClient.get()
                    .uri("")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (feedContent == null || feedContent.isEmpty()) {
                    throw new RssParsingException("Empty RSS feed content received from OpenAI Blog");
                }

                feedContent = normalizeXmlContent(feedContent);

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new StringReader(feedContent));

                log.debug("Successfully parsed OpenAI Blog RSS feed. Found {} entries", feed.getEntries().size());

                return feed.getEntries().stream()
                    .map(this::convertToRssFeedItem)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Failed to parse OpenAI Blog RSS feed", e);
                throw new RssParsingException("OpenAI Blog RSS parsing failed", e);
            }
        });
    }

    private RssFeedItem convertToRssFeedItem(SyndEntry entry) {
        LocalDateTime publishedDate = null;
        if (entry.getPublishedDate() != null) {
            publishedDate = LocalDateTime.ofInstant(
                entry.getPublishedDate().toInstant(), ZoneId.systemDefault());
        } else if (entry.getUpdatedDate() != null) {
            publishedDate = LocalDateTime.ofInstant(
                entry.getUpdatedDate().toInstant(), ZoneId.systemDefault());
        }

        String category = entry.getCategories().isEmpty()
            ? null
            : entry.getCategories().stream()
                .map(SyndCategory::getName)
                .collect(Collectors.joining(","));

        return RssFeedItem.builder()
            .title(entry.getTitle())
            .link(entry.getLink())
            .description(entry.getDescription() != null ? entry.getDescription().getValue() : null)
            .publishedDate(publishedDate)
            .author(entry.getAuthor())
            .category(category)
            .guid(entry.getUri() != null ? entry.getUri() : entry.getLink())
            .imageUrl(null)
            .build();
    }

    @Override
    public String getSourceName() { return "OpenAI Blog"; }

    @Override
    public String getFeedUrl() {
        RssProperties.RssSourceConfig config = properties.getSources().get("openai-blog");
        return config != null ? config.getFeedUrl() : null;
    }
}
```

#### 5.2.3 GoogleAiBlogRssParser

`OpenAiBlogRssParser`와 동일 구조. 설정 키: `google-ai-blog`

```java
@Component
@Slf4j
public class GoogleAiBlogRssParser implements RssParser {
    // OpenAiBlogRssParser와 동일 패턴
    // 설정 키: "google-ai-blog"
    // Google RSS는 media:content (이미지), author (이름/직책) 등 추가 필드 제공
    // SyndEntry.getEnclosures()로 이미지 URL 추출 가능

    private RssFeedItem convertToRssFeedItem(SyndEntry entry) {
        // ... 기본 변환 동일 ...

        // Google RSS 추가 필드: 이미지 URL
        String imageUrl = null;
        if (!entry.getEnclosures().isEmpty()) {
            imageUrl = entry.getEnclosures().get(0).getUrl();
        }

        return RssFeedItem.builder()
            .title(entry.getTitle())
            .link(entry.getLink())
            .description(entry.getDescription() != null ? entry.getDescription().getValue() : null)
            .publishedDate(publishedDate)
            .author(entry.getAuthor())
            .category(category)
            .guid(entry.getUri())
            .imageUrl(imageUrl)
            .build();
    }

    @Override
    public String getSourceName() { return "Google AI Blog"; }
}
```

#### 5.2.4 RSS 설정 추가 (application-rss.yml)

```yaml
rss:
  sources:
    # 기존 소스...
    openai-blog:
      feed-url: https://openai.com/blog/rss.xml
      feed-format: RSS_2.0
      update-frequency: 일일
    google-ai-blog:
      feed-url: https://blog.google/technology/ai/rss/
      feed-format: RSS_2.0
      update-frequency: 일일
```

### 5.3 Batch Job - RSS 수집

#### 5.3.1 EmergingTechRssJobConfig

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmergingTechRssJobConfig {

    private static final String JOB_NAME = Constants.EMERGING_TECH_RSS;
    private static final String STEP1_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final EmergingTechRssService emergingTechRssService;
    private final EmergingTechInternalContract emergingTechInternalApi;

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository,
                   @Qualifier(STEP1_NAME) Step step1,
                   @Qualifier(JOB_NAME + ".listener") EmergingTechRssJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step1)
            .incrementer(new EmergingTechRssIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = STEP1_NAME)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP1_NAME + Constants.ITEM_READER) EmergingTechRssPagingItemReader reader,
                      @Qualifier(STEP1_NAME + Constants.ITEM_PROCESSOR) EmergingTechRssProcessor processor,
                      @Qualifier(STEP1_NAME + Constants.ITEM_WRITER) EmergingTechRssWriter writer) {
        return new StepBuilder(STEP1_NAME, jobRepository)
            .<RssFeedItem, EmergingTechCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
```

#### 5.3.2 EmergingTechRssPagingItemReader

```java
public class EmergingTechRssPagingItemReader extends AbstractPagingItemReader<RssFeedItem> {

    private final EmergingTechRssService rssService;
    private List<RssFeedItem> allItems;

    @Override
    protected void doReadPage() {
        if (allItems == null) {
            allItems = rssService.fetchAllEmergingTechFeeds();
        }

        int start = getPage() * getPageSize();
        int end = Math.min(start + getPageSize(), allItems.size());

        if (start >= allItems.size()) {
            results = Collections.emptyList();
            return;
        }
        results = new ArrayList<>(allItems.subList(start, end));
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();
        allItems = null;
    }
}
```

#### 5.3.3 EmergingTechRssService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmergingTechRssService {

    private final OpenAiBlogRssParser openAiParser;
    private final GoogleAiBlogRssParser googleAiParser;

    /**
     * OpenAI + Google 등 기술 블로그 RSS 피드를 모두 수집
     */
    public List<RssFeedItem> fetchAllEmergingTechFeeds() {
        List<RssFeedItem> allItems = new ArrayList<>();

        try {
            List<RssFeedItem> openAiItems = openAiParser.parse();
            log.info("OpenAI Blog RSS: {} items fetched", openAiItems.size());
            allItems.addAll(openAiItems);
        } catch (Exception e) {
            log.error("Failed to fetch OpenAI Blog RSS", e);
        }

        try {
            List<RssFeedItem> googleItems = googleAiParser.parse();
            log.info("Google AI Blog RSS: {} items fetched", googleItems.size());
            allItems.addAll(googleItems);
        } catch (Exception e) {
            log.error("Failed to fetch Google AI Blog RSS", e);
        }

        return allItems;
    }
}
```

#### 5.3.4 EmergingTechRssProcessor

```java
public class EmergingTechRssProcessor implements ItemProcessor<RssFeedItem, EmergingTechCreateRequest> {

    @Override
    public EmergingTechCreateRequest process(RssFeedItem item) {
        if (item.title() == null || item.link() == null) {
            return null;
        }

        // RSS 소스에서 Provider 판별
        TechProvider provider = resolveProvider(item.link());

        return EmergingTechCreateRequest.builder()
            .provider(provider)
            .updateType(classifyUpdateType(item))
            .title(item.title())
            .summary(truncateHtml(item.description(), 500))
            .url(item.link())
            .publishedAt(item.publishedDate())
            .sourceType(SourceType.RSS)
            .status(PostStatus.DRAFT)
            .externalId("rss:" + generateHash(item.guid() != null ? item.guid() : item.link()))
            .metadata(Map.of(
                "author", Objects.requireNonNullElse(item.author(), ""),
                "category", Objects.requireNonNullElse(item.category(), ""),
                "tags", extractTags(item.category())
            ))
            .build();
    }

    private TechProvider resolveProvider(String url) {
        if (url.contains("openai.com")) return TechProvider.OPENAI;
        if (url.contains("blog.google")) return TechProvider.GOOGLE;
        throw new IllegalArgumentException("Unknown RSS source URL: " + url);
    }

    /**
     * RSS 카테고리/제목 기반으로 업데이트 타입 자동 분류
     */
    private EmergingTechType classifyUpdateType(RssFeedItem item) {
        String title = item.title().toLowerCase();
        String category = item.category() != null ? item.category().toLowerCase() : "";

        if (title.contains("api") || category.contains("api")) return EmergingTechType.API_UPDATE;
        if (title.contains("release") || title.contains("introducing") || title.contains("model")
                || title.contains("announcing")) return EmergingTechType.MODEL_RELEASE;
        if (title.contains("launch") || title.contains("product") || title.contains("available")
                || title.contains("new service")) return EmergingTechType.PRODUCT_LAUNCH;
        if (title.contains("platform") || title.contains("cloud") || title.contains("infrastructure")
                || title.contains("update") || category.contains("platform")) return EmergingTechType.PLATFORM_UPDATE;
        return EmergingTechType.BLOG_POST;
    }
}
```

---

## 6. 웹 크롤링 설계 (Anthropic, Meta AI, xAI)

### 6.1 대상 페이지 분석

| Provider | URL | 렌더링 | HTML 구조 | Selenium 필요 |
|----------|-----|--------|----------|--------------|
| Anthropic | `https://www.anthropic.com/news` | Next.js SSR | `<h2>/<h3>` + `<a href="/news/[slug]">` | 불필요 (초기 페이지) |
| Meta AI | `https://ai.meta.com/blog/` | React SPA | `<h2>/<h3>` + `<a href="/blog/[slug]/">` | 필요 가능성 높음 |
| xAI | `https://x.ai/news` | Next.js App Router SSR + Cloudflare | `<h3>/<h4>` + `<a href="/news/[slug]">` | **필수** (Cloudflare 403 차단) |

#### Anthropic News 페이지 상세

- **SSR 렌더링**: 초기 HTML에 기사 데이터 포함 → Jsoup으로 파싱 가능
- **타이틀**: `<h2>` 또는 `<h3>` 태그, `StyreneB` 폰트 클래스
- **링크**: `<a href="/news/[slug]">` 패턴
- **날짜**: ISO-8601 형식 (`publishedOn` 필드), `<time>` 또는 `<span>` 태그
- **요약**: `summary` 필드, `<p>` 또는 텍스트 노드
- **페이지네이션**: "See more" 버튼 (JavaScript 기반 → 추가 로딩 시 별도 처리 필요)
- **대안**: Next.js `_next/data/[buildId]/news.json` 엔드포인트 직접 호출 검토

#### Meta AI Blog 페이지 상세

- **React SPA**: Meta Bootloader 시스템, 동적 컴포넌트 로딩
- **타이틀**: `<h2>/<h3>` 안의 `<a>` 태그
- **링크**: `/blog/[article-slug]/` 패턴
- **날짜**: `"Month DD, YYYY"` 형식, `<span>` 또는 `<time>` 태그
- **요약**: 타이틀 아래 1~2 문장 텍스트
- **페이지네이션**: "Next" 버튼 (서버사이드 페이지네이션)
- **SPA 대응**: Selenium/Playwright로 렌더링 후 파싱 필요

#### xAI News 페이지 상세

- **Next.js App Router SSR**: React Server Components 사용, 콘텐츠가 초기 HTML에 포함됨
- **Cloudflare 보호**: 모든 HTTP 클라이언트(curl, WebClient, Jsoup) 403 차단 → Selenium 필수
- **CSS 프레임워크**: Tailwind CSS (유틸리티 클래스만 사용, 시맨틱 클래스 없음)
- **타이틀**: Featured 기사 `<h3>`, Grid 기사 `<h4>`, 모두 `<a href="/news/[slug]">` 내부
- **링크**: `/news/[slug]` 패턴 (예: `/news/grok-4-1`, `/news/series-e`)
- **날짜**: `"Month DD, YYYY"` 형식, `p.mono-tag.text-xs` 또는 `span.mono-tag.text-xs` 태그
- **요약**: `p.text-secondary` 클래스 (line-clamp-3 적용)
- **태그**: `span.mono-tag.text-xs` (예: "grok", "api", "enterprise")
- **페이지네이션**: 없음 - 전체 기사 (~24개)가 단일 페이지에 로드
- **레이아웃**: 1개 Featured 기사 (hero) + 3열 Grid 카드
- **CSS 선택자**:
  - 기사 링크: `a[href*="/news/"]`
  - 타이틀: `a[href*="/news/"] h3, a[href*="/news/"] h4`
  - 요약: `div.group.relative p.text-secondary`
  - 날짜: `p.mono-tag.text-xs, span.mono-tag.text-xs`

### 6.2 client-scraper 모듈 확장

#### 6.2.1 기술 블로그 수집용 DTO

기존 `ScrapedContestItem`은 대회 전용이므로, 기술 블로그 수집용 DTO 추가:

```java
@Builder
public record ScrapedTechArticle(
    /** 기사 제목 */
    String title,
    /** 기사 URL */
    String url,
    /** 기사 요약/설명 */
    String summary,
    /** 발행일시 */
    LocalDateTime publishedDate,
    /** 작성자 */
    String author,
    /** 카테고리/태그 */
    String category,
    /** Provider 이름 (ANTHROPIC, META, XAI) */
    String providerName
) {}
```

#### 6.2.2 기술 블로그 스크래퍼 인터페이스

기존 `WebScraper`는 `List<ScrapedContestItem>`을 반환하므로, 기술 블로그 전용 인터페이스 추가:

```java
public interface TechBlogScraper {

    /**
     * 기술 블로그 페이지를 스크래핑하여 기사 리스트 반환
     */
    List<ScrapedTechArticle> scrapeArticles();

    /**
     * Provider 이름 반환
     */
    String getProviderName();

    /**
     * 기본 URL 반환
     */
    String getBaseUrl();
}
```

#### 6.2.3 AnthropicNewsScraper

```java
@Component
@Slf4j
public class AnthropicNewsScraper implements TechBlogScraper {

    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScraperProperties properties;
    private final RetryRegistry retryRegistry;

    private static final String NEWS_PATH = "/news";
    private static final String BASE_URL_KEY = "anthropic-news";

    public AnthropicNewsScraper(
            @Qualifier("scraperWebClientBuilder") WebClient.Builder webClientBuilder,
            RobotsTxtChecker robotsTxtChecker,
            ScraperProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.robotsTxtChecker = robotsTxtChecker;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public List<ScrapedTechArticle> scrapeArticles() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(BASE_URL_KEY);
        if (config == null) {
            throw new ScrapingException("Anthropic News scraper source configuration not found");
        }

        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), NEWS_PATH)) {
            throw new ScrapingException("Scraping not allowed by robots.txt for Anthropic News");
        }

        WebClient webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        Retry retry = retryRegistry.retry("scraperRetry");

        return retry.executeSupplier(() -> {
            try {
                log.debug("Scraping Anthropic News: {}", config.getBaseUrl() + NEWS_PATH);

                String html = webClient.get()
                    .uri(NEWS_PATH)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (html == null || html.isEmpty()) {
                    throw new ScrapingException("Empty HTML content from Anthropic News");
                }

                Document doc = Jsoup.parse(html);
                List<ScrapedTechArticle> articles = new ArrayList<>();

                // Next.js SSR 페이지 - 기사 카드 추출
                // 기본 선택자: 기사 링크를 포함하는 요소
                var articleElements = doc.select("a[href^=/news/]");

                for (Element element : articleElements) {
                    try {
                        ScrapedTechArticle article = extractArticle(element, config.getBaseUrl());
                        if (article != null) {
                            articles.add(article);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract Anthropic article: {}", e.getMessage());
                    }
                }

                log.info("Scraped {} articles from Anthropic News", articles.size());
                return articles;
            } catch (Exception e) {
                log.error("Anthropic News scraping failed", e);
                throw new ScrapingException("Anthropic News scraping failed", e);
            }
        });
    }

    private ScrapedTechArticle extractArticle(Element element, String baseUrl) {
        String href = element.attr("href");
        String url = href.startsWith("http") ? href : baseUrl + href;

        // 부모/자식 요소에서 타이틀, 날짜, 요약 추출
        Element parent = element.parent();
        String title = extractText(element, "h2, h3, h4");
        if (title == null || title.isEmpty()) return null;

        String summary = extractText(parent, "p");
        String dateText = extractText(parent, "time, span");
        LocalDateTime publishedDate = parseDateText(dateText);

        return ScrapedTechArticle.builder()
            .title(title)
            .url(url)
            .summary(summary)
            .publishedDate(publishedDate)
            .author(null)
            .category(null)
            .providerName(TechProvider.ANTHROPIC.name())
            .build();
    }

    private String extractText(Element parent, String selector) {
        if (parent == null) return null;
        Element el = parent.select(selector).first();
        return el != null ? el.text() : null;
    }

    @Override
    public String getProviderName() { return "Anthropic"; }

    @Override
    public String getBaseUrl() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(BASE_URL_KEY);
        return config != null ? config.getBaseUrl() : null;
    }
}
```

#### 6.2.4 MetaAiBlogScraper

```java
@Component
@Slf4j
public class MetaAiBlogScraper implements TechBlogScraper {

    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScraperProperties properties;
    private final RetryRegistry retryRegistry;
    // Selenium WebDriver (SPA 렌더링용, 선택적 사용)
    private final Optional<WebDriver> webDriver;

    private static final String BLOG_PATH = "/blog/";
    private static final String BASE_URL_KEY = "meta-ai-blog";

    @Override
    public List<ScrapedTechArticle> scrapeArticles() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(BASE_URL_KEY);
        if (config == null) {
            throw new ScrapingException("Meta AI Blog scraper source configuration not found");
        }

        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), BLOG_PATH)) {
            throw new ScrapingException("Scraping not allowed by robots.txt for Meta AI Blog");
        }

        Retry retry = retryRegistry.retry("scraperRetry");

        return retry.executeSupplier(() -> {
            try {
                String html;

                if (config.isRequiresSelenium() && webDriver.isPresent()) {
                    // SPA: Selenium으로 렌더링 후 HTML 추출
                    html = fetchWithSelenium(config.getBaseUrl() + BLOG_PATH);
                } else {
                    // Fallback: WebClient로 시도
                    html = fetchWithWebClient(config.getBaseUrl(), BLOG_PATH);
                }

                if (html == null || html.isEmpty()) {
                    throw new ScrapingException("Empty HTML content from Meta AI Blog");
                }

                Document doc = Jsoup.parse(html);
                List<ScrapedTechArticle> articles = new ArrayList<>();

                // 기사 링크 추출
                var articleElements = doc.select("a[href*=/blog/]");

                for (Element element : articleElements) {
                    try {
                        ScrapedTechArticle article = extractArticle(element, config.getBaseUrl());
                        if (article != null) {
                            articles.add(article);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract Meta AI article: {}", e.getMessage());
                    }
                }

                log.info("Scraped {} articles from Meta AI Blog", articles.size());
                return articles;
            } catch (Exception e) {
                log.error("Meta AI Blog scraping failed", e);
                throw new ScrapingException("Meta AI Blog scraping failed", e);
            }
        });
    }

    /**
     * Selenium WebDriver로 SPA 페이지 렌더링 후 HTML 추출
     */
    private String fetchWithSelenium(String url) {
        WebDriver driver = webDriver.orElseThrow(() ->
            new ScrapingException("Selenium WebDriver not available for Meta AI Blog"));

        try {
            driver.get(url);

            // 페이지 로딩 대기 (React 렌더링 완료까지)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));

            // 추가 대기: 동적 콘텐츠 로딩
            Thread.sleep(3000);

            return driver.getPageSource();
        } catch (Exception e) {
            log.error("Selenium fetch failed for: {}", url, e);
            throw new ScrapingException("Selenium fetch failed for Meta AI Blog", e);
        }
    }

    private String fetchWithWebClient(String baseUrl, String path) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    private ScrapedTechArticle extractArticle(Element element, String baseUrl) {
        String href = element.attr("href");
        if (href.equals("/blog/") || href.equals("/blog")) return null;

        String url = href.startsWith("http") ? href : baseUrl + href;

        Element parent = element.parent();
        String title = extractText(element, "h2, h3, h4");
        if (title == null || title.isEmpty()) {
            title = element.text();
        }
        if (title == null || title.isEmpty()) return null;

        String summary = extractText(parent, "p, div:not(:has(*))");
        String dateText = extractText(parent, "time, span");
        LocalDateTime publishedDate = parseDateText(dateText);

        return ScrapedTechArticle.builder()
            .title(title)
            .url(url)
            .summary(summary)
            .publishedDate(publishedDate)
            .author(null)
            .category(null)
            .providerName(TechProvider.META.name())
            .build();
    }

    @Override
    public String getProviderName() { return "Meta AI"; }

    @Override
    public String getBaseUrl() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(BASE_URL_KEY);
        return config != null ? config.getBaseUrl() : null;
    }
}
```

#### 6.2.5 XaiNewsScraper

```java
@Component
@Slf4j
public class XaiNewsScraper implements TechBlogScraper {

    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScraperProperties properties;
    private final RetryRegistry retryRegistry;
    private final Optional<WebDriver> webDriver;

    private static final String NEWS_PATH = "/news";
    private static final String BASE_URL_KEY = "xai-news";

    public XaiNewsScraper(
            @Qualifier("scraperWebClientBuilder") WebClient.Builder webClientBuilder,
            RobotsTxtChecker robotsTxtChecker,
            ScraperProperties properties,
            RetryRegistry retryRegistry,
            Optional<WebDriver> webDriver) {
        this.webClientBuilder = webClientBuilder;
        this.robotsTxtChecker = robotsTxtChecker;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
        this.webDriver = webDriver;
    }

    @Override
    public List<ScrapedTechArticle> scrapeArticles() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(BASE_URL_KEY);
        if (config == null) {
            throw new ScrapingException("xAI News scraper source configuration not found");
        }

        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), NEWS_PATH)) {
            throw new ScrapingException("Scraping not allowed by robots.txt for xAI News");
        }

        Retry retry = retryRegistry.retry("scraperRetry");

        return retry.executeSupplier(() -> {
            try {
                // Cloudflare 보호로 Selenium 필수
                String html = fetchWithSelenium(config.getBaseUrl() + NEWS_PATH);

                if (html == null || html.isEmpty()) {
                    throw new ScrapingException("Empty HTML content from xAI News");
                }

                Document doc = Jsoup.parse(html);
                List<ScrapedTechArticle> articles = new ArrayList<>();

                // xAI News 페이지 - 기사 링크 추출
                var articleElements = doc.select("a[href*=/news/]");

                for (Element element : articleElements) {
                    try {
                        ScrapedTechArticle article = extractArticle(element, config.getBaseUrl());
                        if (article != null) {
                            articles.add(article);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract xAI article: {}", e.getMessage());
                    }
                }

                log.info("Scraped {} articles from xAI News", articles.size());
                return articles;
            } catch (Exception e) {
                log.error("xAI News scraping failed", e);
                throw new ScrapingException("xAI News scraping failed", e);
            }
        });
    }

    private String fetchWithSelenium(String url) {
        WebDriver driver = webDriver.orElseThrow(() ->
            new ScrapingException("Selenium WebDriver not available for xAI News (Cloudflare requires browser)"));

        try {
            driver.get(url);

            // Cloudflare 챌린지 통과 대기
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));

            // Cloudflare 챌린지 완료 후 추가 대기
            Thread.sleep(5000);

            return driver.getPageSource();
        } catch (Exception e) {
            log.error("Selenium fetch failed for xAI News: {}", url, e);
            throw new ScrapingException("Selenium fetch failed for xAI News", e);
        }
    }

    private ScrapedTechArticle extractArticle(Element element, String baseUrl) {
        String href = element.attr("href");
        if (href.equals("/news") || href.equals("/news/")) return null;

        String url = href.startsWith("http") ? href : baseUrl + href;

        // Tailwind CSS 기반 구조에서 타이틀 추출
        String title = extractText(element, "h3, h4");
        if (title == null || title.isEmpty()) {
            title = element.text();
        }
        if (title == null || title.isEmpty()) return null;

        // 부모 요소에서 날짜, 요약 추출
        Element parent = element.parent();
        String summary = extractText(parent, "p.text-secondary");
        String dateText = extractText(parent, "p.mono-tag, span.mono-tag");
        LocalDateTime publishedDate = parseDateText(dateText);

        // 태그 추출
        String category = null;
        Element tagElement = parent != null ? parent.select("span.mono-tag").first() : null;
        if (tagElement != null) {
            category = tagElement.text();
        }

        return ScrapedTechArticle.builder()
            .title(title)
            .url(url)
            .summary(summary)
            .publishedDate(publishedDate)
            .author(null)
            .category(category)
            .providerName(TechProvider.XAI.name())
            .build();
    }

    private String extractText(Element parent, String selector) {
        if (parent == null) return null;
        Element el = parent.select(selector).first();
        return el != null ? el.text() : null;
    }

    @Override
    public String getProviderName() { return "xAI"; }

    @Override
    public String getBaseUrl() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(BASE_URL_KEY);
        return config != null ? config.getBaseUrl() : null;
    }
}
```

#### 6.2.6 Selenium 설정

```java
@Configuration
@ConditionalOnProperty(name = "scraper.selenium.enabled", havingValue = "true")
public class SeleniumConfig {

    @Value("${scraper.selenium.driver-path:}")
    private String driverPath;

    @Value("${scraper.selenium.headless:true}")
    private boolean headless;

    @Bean
    public WebDriver seleniumWebDriver() {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--user-agent=ShrimpTM-Demo/1.0");

        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
        }

        return new ChromeDriver(options);
    }

    @PreDestroy
    public void cleanup() {
        // WebDriver 종료
    }
}
```

#### 6.2.7 Scraper 설정 추가 (application-scraper.yml)

```yaml
scraper:
  sources:
    # 기존 소스...
    anthropic-news:
      base-url: https://www.anthropic.com
      data-format: HTML
      min-interval-seconds: 2
      requires-selenium: false
    meta-ai-blog:
      base-url: https://ai.meta.com
      data-format: HTML
      min-interval-seconds: 2
      requires-selenium: true
    xai-news:
      base-url: https://x.ai
      data-format: HTML
      min-interval-seconds: 2
      requires-selenium: true

  selenium:
    enabled: false  # 로컬 개발 시 false, 프로덕션에서 true
    driver-path: ""  # ChromeDriver 경로 (자동 감지 시 빈 문자열)
    headless: true
```

### 6.3 Batch Job - 웹 크롤링 수집

#### 6.3.1 EmergingTechScraperJobConfig

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmergingTechScraperJobConfig {

    private static final String JOB_NAME = Constants.EMERGING_TECH_SCRAPER;
    private static final String STEP1_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final EmergingTechScraperService scraperService;
    private final EmergingTechInternalContract emergingTechInternalApi;

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository,
                   @Qualifier(STEP1_NAME) Step step1,
                   @Qualifier(JOB_NAME + ".listener") EmergingTechScraperJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step1)
            .incrementer(new EmergingTechScraperIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = STEP1_NAME)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP1_NAME + Constants.ITEM_READER) EmergingTechScrapingItemReader reader,
                      @Qualifier(STEP1_NAME + Constants.ITEM_PROCESSOR) EmergingTechScraperProcessor processor,
                      @Qualifier(STEP1_NAME + Constants.ITEM_WRITER) EmergingTechScraperWriter writer) {
        return new StepBuilder(STEP1_NAME, jobRepository)
            .<ScrapedTechArticle, EmergingTechCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
```

#### 6.3.2 EmergingTechScrapingItemReader

```java
public class EmergingTechScrapingItemReader extends AbstractPagingItemReader<ScrapedTechArticle> {

    private final EmergingTechScraperService scraperService;
    private List<ScrapedTechArticle> allItems;

    @Override
    protected void doReadPage() {
        if (allItems == null) {
            allItems = scraperService.scrapeAllSources();
        }

        int start = getPage() * getPageSize();
        int end = Math.min(start + getPageSize(), allItems.size());

        if (start >= allItems.size()) {
            results = Collections.emptyList();
            return;
        }
        results = new ArrayList<>(allItems.subList(start, end));
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();
        allItems = null;
    }
}
```

#### 6.3.3 EmergingTechScraperService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmergingTechScraperService {

    private final AnthropicNewsScraper anthropicScraper;
    private final MetaAiBlogScraper metaScraper;
    private final XaiNewsScraper xaiScraper;

    /**
     * Anthropic + Meta + xAI 등 기술 블로그를 모두 크롤링
     */
    public List<ScrapedTechArticle> scrapeAllSources() {
        List<ScrapedTechArticle> allArticles = new ArrayList<>();

        try {
            List<ScrapedTechArticle> anthropicArticles = anthropicScraper.scrapeArticles();
            log.info("Anthropic News: {} articles scraped", anthropicArticles.size());
            allArticles.addAll(anthropicArticles);
        } catch (Exception e) {
            log.error("Failed to scrape Anthropic News", e);
        }

        try {
            List<ScrapedTechArticle> metaArticles = metaScraper.scrapeArticles();
            log.info("Meta AI Blog: {} articles scraped", metaArticles.size());
            allArticles.addAll(metaArticles);
        } catch (Exception e) {
            log.error("Failed to scrape Meta AI Blog", e);
        }

        try {
            List<ScrapedTechArticle> xaiArticles = xaiScraper.scrapeArticles();
            log.info("xAI News: {} articles scraped", xaiArticles.size());
            allArticles.addAll(xaiArticles);
        } catch (Exception e) {
            log.error("Failed to scrape xAI News", e);
        }

        return allArticles;
    }
}
```

#### 6.3.4 EmergingTechScraperProcessor

```java
public class EmergingTechScraperProcessor implements ItemProcessor<ScrapedTechArticle, EmergingTechCreateRequest> {

    @Override
    public EmergingTechCreateRequest process(ScrapedTechArticle article) {
        if (article.title() == null || article.url() == null) {
            return null;
        }

        return EmergingTechCreateRequest.builder()
            .provider(TechProvider.valueOf(article.providerName()))
            .updateType(classifyUpdateType(article))
            .title(article.title())
            .summary(article.summary())
            .url(article.url())
            .publishedAt(article.publishedDate())
            .sourceType(SourceType.WEB_SCRAPING)
            .status(PostStatus.DRAFT)
            .externalId("scraper:" + generateHash(article.url()))
            .metadata(Map.of(
                "author", Objects.requireNonNullElse(article.author(), ""),
                "category", Objects.requireNonNullElse(article.category(), "")
            ))
            .build();
    }

    private EmergingTechType classifyUpdateType(ScrapedTechArticle article) {
        String title = article.title().toLowerCase();
        String category = article.category() != null ? article.category().toLowerCase() : "";

        if (title.contains("api") || category.contains("api")) return EmergingTechType.API_UPDATE;
        if (title.contains("model") || title.contains("release") || title.contains("introducing")
                || title.contains("announcing")) return EmergingTechType.MODEL_RELEASE;
        if (title.contains("launch") || title.contains("product") || title.contains("available")
                || title.contains("new service")) return EmergingTechType.PRODUCT_LAUNCH;
        if (title.contains("platform") || title.contains("cloud") || title.contains("infrastructure")
                || title.contains("update") || category.contains("platform")) return EmergingTechType.PLATFORM_UPDATE;
        return EmergingTechType.BLOG_POST;
    }
}
```

---

## 7. Feign Client 설계

### 7.1 GitHub Releases API 추가

```java
// GitHubFeignClient.java에 추가
@GetMapping("/repos/{owner}/{repo}/releases")
List<GitHubDto.Release> getReleases(
    @RequestHeader(value = "Authorization", required = false) String token,
    @PathVariable("owner") String owner,
    @PathVariable("repo") String repo,
    @RequestParam(value = "per_page", defaultValue = "10") Integer perPage
);
```

### 7.2 GitHub DTO 추가

```java
// GitHubDto.java에 추가
@Builder
public record Release(
    @JsonProperty("id") Long id,
    @JsonProperty("tag_name") String tagName,
    @JsonProperty("name") String name,
    @JsonProperty("body") String body,
    @JsonProperty("html_url") String htmlUrl,
    @JsonProperty("published_at") LocalDateTime publishedAt,
    @JsonProperty("prerelease") Boolean prerelease,
    @JsonProperty("draft") Boolean draft
) {}
```

### 7.3 내부 API Contract

```java
public interface EmergingTechInternalContract {

    @PostMapping("/api/v1/emerging-tech/internal")
    ApiResponse<EmergingTechDetailResponse> createInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody EmergingTechCreateRequest request
    );

    @PostMapping("/api/v1/emerging-tech/internal/batch")
    ApiResponse<EmergingTechBatchResponse> createBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody EmergingTechBatchRequest request
    );
}
```

---

## 8. API 엔드포인트 설계

### 8.1 엔드포인트 목록

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/v1/emerging-tech | 목록 조회 | JWT |
| GET | /api/v1/emerging-tech/{id} | 상세 조회 | JWT |
| GET | /api/v1/emerging-tech/search | 검색 | JWT |
| POST | /api/v1/emerging-tech/internal | 단건 생성 | X-Internal-Api-Key |
| POST | /api/v1/emerging-tech/internal/batch | 다건 생성 | X-Internal-Api-Key |
| POST | /api/v1/emerging-tech/{id}/approve | 승인 | X-Internal-Api-Key |
| POST | /api/v1/emerging-tech/{id}/reject | 거부 | X-Internal-Api-Key |

### 8.2 Request/Response DTO

```java
// 목록 조회 요청
public record EmergingTechListRequest(
    @Min(0) int page,
    @Min(1) @Max(100) int size,
    TechProvider provider,      // 필터 (선택)
    EmergingTechType updateType,  // 필터 (선택)
    PostStatus status         // 필터 (선택)
) {}

// 목록 조회 응답
public record EmergingTechListResponse(
    List<EmergingTechSummary> items,
    PageInfo pageInfo
) {}

public record EmergingTechSummary(
    String id,
    String provider,
    String updateType,
    String title,
    String url,
    LocalDateTime publishedAt,
    String status
) {}

// 생성 요청
public record EmergingTechCreateRequest(
    @NotNull TechProvider provider,
    @NotNull EmergingTechType updateType,
    @NotBlank String title,
    String summary,
    @NotBlank String url,
    LocalDateTime publishedAt,
    @NotNull SourceType sourceType,
    @NotNull PostStatus status,
    String externalId,
    Map<String, Object> metadata
) {}
```

---

## 9. Slack 알림 통합

### 9.1 알림 서비스

```java
@Service
@RequiredArgsConstructor
public class EmergingTechSlackService {

    private final SlackClient slackClient;

    @Value("${slack.emerging-tech.channel}")
    private String channel;

    public void sendEmergingTechNotification(EmergingTechDetailResponse update) {
        String message = buildMessage(update);
        slackClient.sendMessage(channel, message);
    }

    private String buildMessage(EmergingTechDetailResponse update) {
        return String.format("""
            :sparkles: *새로운 기술 업데이트*

            *Provider:* %s
            *Type:* %s
            *Title:* %s
            *URL:* %s
            *Published:* %s

            > %s
            """,
            update.provider(),
            update.updateType(),
            update.title(),
            update.url(),
            update.publishedAt(),
            truncate(update.summary(), 200)
        );
    }
}
```

### 9.2 설정

```yaml
# application.yml
slack:
  emerging-tech:
    channel: "#emerging-tech"
    enabled: true
```

---

## 10. 설정 파일 구조

### 10.1 application-emerging-tech.yml

```yaml
emerging-tech:
  github:
    repositories:
      - owner: openai
        repo: openai-python
      - owner: anthropics
        repo: anthropic-sdk-python
      - owner: google
        repo: generative-ai-python
      - owner: facebookresearch
        repo: llama
      - owner: xai-org
        repo: grok-1

  schedule:
    github:
      cron: "0 0 */6 * * *"    # 6시간마다
    rss:
      cron: "0 15 */6 * * *"   # 6시간마다 (GitHub 15분 후)
    scraper:
      cron: "0 30 */6 * * *"   # 6시간마다 (RSS 15분 후)

internal-api:
  emerging-tech:
    api-key: ${EMERGING_TECH_INTERNAL_API_KEY:default-emerging-tech-key}
```

### 10.2 application-rss.yml 추가 항목

```yaml
rss:
  sources:
    # 기존 소스 유지...
    openai-blog:
      feed-url: https://openai.com/blog/rss.xml
      feed-format: RSS_2.0
      update-frequency: 일일
    google-ai-blog:
      feed-url: https://blog.google/technology/ai/rss/
      feed-format: RSS_2.0
      update-frequency: 일일
```

### 10.3 application-scraper.yml 추가 항목

```yaml
scraper:
  sources:
    # 기존 소스 유지...
    anthropic-news:
      base-url: https://www.anthropic.com
      data-format: HTML
      min-interval-seconds: 2
      requires-selenium: false
    meta-ai-blog:
      base-url: https://ai.meta.com
      data-format: HTML
      min-interval-seconds: 2
      requires-selenium: true
    xai-news:
      base-url: https://x.ai
      data-format: HTML
      min-interval-seconds: 2
      requires-selenium: true

  selenium:
    enabled: false
    driver-path: ""
    headless: true
```

---

## 11. 시퀀스 다이어그램

### 11.1 RSS 수집 플로우 (OpenAI, Google)

```
┌──────────┐     ┌───────────┐     ┌────────────┐     ┌──────────────┐     ┌─────────┐
│Scheduler │     │ RSS Job   │     │ RssParser  │     │ InternalAPI  │     │ MongoDB │
└────┬─────┘     └─────┬─────┘     └──────┬─────┘     └──────┬───────┘     └────┬────┘
     │ trigger         │                   │                  │                 │
     │────────────────>│                   │                  │                 │
     │                 │                   │                  │                 │
     │                 │ parse RSS feeds   │                  │                 │
     │                 │──────────────────>│                  │                 │
     │                 │                   │                  │                 │
     │                 │ List<RssFeedItem> │                  │                 │
     │                 │<──────────────────│                  │                 │
     │                 │                   │                  │                 │
     │                 │ process → EmergingTechCreateRequest      │                 │
     │                 │─────────────────────────────────────>│                 │
     │                 │                   │                  │                 │
     │                 │                   │                  │ save            │
     │                 │                   │                  │────────────────>│
     │                 │                   │                  │                 │
```

### 11.2 웹 크롤링 플로우 (Anthropic, Meta, xAI)

```
┌──────────┐     ┌────────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────┐
│Scheduler │     │ ScraperJob │     │TechBlogScraper│    │ InternalAPI  │     │ MongoDB │
└────┬─────┘     └─────┬──────┘     └──────┬──────┘     └──────┬───────┘     └────┬────┘
     │ trigger         │                    │                   │                 │
     │────────────────>│                    │                   │                 │
     │                 │                    │                   │                 │
     │                 │ scrapeArticles()   │                   │                 │
     │                 │───────────────────>│                   │                 │
     │                 │                    │                   │                 │
     │                 │                    │ [Meta: Selenium]  │                 │
     │                 │                    │ [xAI: Selenium]   │                 │
     │                 │                    │ [Anthropic: Jsoup]│                 │
     │                 │                    │                   │                 │
     │                 │ List<ScrapedTechArticle>                 │                 │
     │                 │<───────────────────│                   │                 │
     │                 │                    │                   │                 │
     │                 │ process → EmergingTechCreateRequest        │                 │
     │                 │──────────────────────────────────────>│                 │
     │                 │                    │                   │                 │
     │                 │                    │                   │ save            │
     │                 │                    │                   │────────────────>│
     │                 │                    │                   │                 │
```

### 11.3 GitHub Releases 플로우

```
┌──────────┐     ┌───────────┐     ┌──────────────┐     ┌──────────────┐     ┌─────────┐
│Scheduler │     │ GitHubJob │     │ GitHubAPI    │     │ InternalAPI  │     │ MongoDB │
└────┬─────┘     └─────┬─────┘     └──────┬───────┘     └──────┬───────┘     └────┬────┘
     │ trigger         │                   │                    │                 │
     │────────────────>│                   │                    │                 │
     │                 │                   │                    │                 │
     │                 │ GET /releases     │                    │                 │
     │                 │──────────────────>│                    │                 │
     │                 │                   │                    │                 │
     │                 │ process & write   │                    │                 │
     │                 │─────────────────────────────────────> │                 │
     │                 │                   │                    │                 │
     │                 │                   │                    │ save            │
     │                 │                   │                    │────────────────>│
     │                 │                   │                    │                 │
```

---

## 12. 의존성 추가 (build.gradle)

### 12.1 client-scraper 모듈

```gradle
dependencies {
    // 기존 의존성 유지...

    // Selenium (SPA 크롤링용)
    implementation 'org.seleniumhq.selenium:selenium-java:4.18.1'
    implementation 'org.seleniumhq.selenium:selenium-chrome-driver:4.18.1'
    implementation 'io.github.bonigarcia:webdrivermanager:5.7.0'
}
```

### 12.2 batch-source 모듈

```gradle
dependencies {
    // 기존 의존성 유지...

    implementation project(':client-rss')       // RSS 수집
    implementation project(':client-scraper')   // 웹 크롤링
}
```

---

## 13. 참고 자료

- GitHub REST API - Releases: https://docs.github.com/en/rest/releases/releases
- Spring Batch Reference: https://docs.spring.io/spring-batch/reference/
- Slack API: https://api.slack.com/messaging/sending
- ROME (RSS/Atom 파서): https://rometools.github.io/rome/
- Jsoup (HTML 파서): https://jsoup.org/
- Selenium WebDriver: https://www.selenium.dev/documentation/webdriver/
- OpenAI Blog RSS: https://openai.com/blog/rss.xml
- Google AI Blog RSS: https://blog.google/technology/ai/rss/
- xAI News: https://x.ai/news
- xAI GitHub (Grok): https://github.com/xai-org/grok-1
