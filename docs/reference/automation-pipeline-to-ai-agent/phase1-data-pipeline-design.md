# Phase 1: 데이터 수집 파이프라인 설계서

## 1. 개요

### 1.1 목적
빅테크 AI 서비스(OpenAI, Anthropic, Google, Meta)의 업데이트 정보를 자동 수집하는 Spring Batch 기반 파이프라인 구축

### 1.2 범위
- 공식 블로그/뉴스 RSS 수집
- GitHub Releases API 연동
- 웹 페이지 크롤링 (RSS 미제공 소스)
- Draft 상태 포스트 저장 및 Slack 알림

---

## 2. 데이터 소스 정의

### 2.1 소스별 수집 방식

| Provider | 소스 | URL | 수집 방식 | 클라이언트 |
|----------|------|-----|----------|-----------|
| OpenAI | Blog | https://openai.com/blog | HTML 크롤링 | client-scraper |
| OpenAI | GitHub | https://github.com/openai/openai-python | GitHub API | client-feign |
| Anthropic | News | https://www.anthropic.com/news | HTML 크롤링 | client-scraper |
| Anthropic | GitHub | https://github.com/anthropics/anthropic-sdk-python | GitHub API | client-feign |
| Google | AI Blog | https://blog.google/technology/ai/ | HTML 크롤링 | client-scraper |
| Meta | AI Blog | https://ai.meta.com/blog/ | HTML 크롤링 | client-scraper |
| Meta | GitHub | https://github.com/facebookresearch/llama | GitHub API | client-feign |

### 2.2 GitHub 대상 저장소

```yaml
ai-update:
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
```

---

## 3. 엔티티/도큐먼트 스키마

### 3.1 AiUpdateEntity (MariaDB - Write Store)

```java
@Entity
@Table(name = "ai_updates")
@Getter
@Setter
public class AiUpdateEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AiProvider provider;  // OPENAI, ANTHROPIC, GOOGLE, META

    @Enumerated(EnumType.STRING)
    @Column(name = "update_type", nullable = false, length = 30)
    private AiUpdateType updateType;  // MODEL_RELEASE, API_UPDATE, SDK_RELEASE, BLOG_POST

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;  // RSS, GITHUB_RELEASE, WEB_SCRAPING

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status;  // DRAFT, PENDING, PUBLISHED, REJECTED

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "external_id", length = 200)
    private String externalId;  // 중복 체크용 (GitHub release ID, URL hash 등)
}
```

### 3.2 Enum 정의

```java
public enum AiProvider {
    OPENAI, ANTHROPIC, GOOGLE, META
}

public enum AiUpdateType {
    MODEL_RELEASE,    // 모델 출시 (GPT-5, Claude 4 등)
    API_UPDATE,       // API 변경사항
    SDK_RELEASE,      // SDK 새 버전
    BLOG_POST         // 일반 블로그 포스트
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

### 3.3 AiUpdateDocument (MongoDB - Read Store)

```java
@Document(collection = "ai_updates")
@Getter
@Setter
public class AiUpdateDocument {

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
    private AiUpdateMetadata metadata;

    @Field("external_id")
    @Indexed(unique = true)
    private String externalId;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    public static class AiUpdateMetadata {
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
batch/source/domain/ai-update/
├── common/
│   ├── dto/
│   │   └── AiUpdateCreateRequest.java
│   └── service/
│       └── AiUpdateApiService.java
├── github/
│   ├── jobconfig/AiUpdateGitHubJobConfig.java
│   ├── reader/GitHubReleasePagingItemReader.java
│   ├── processor/GitHubReleaseProcessor.java
│   ├── writer/GitHubReleaseWriter.java
│   └── listener/GitHubJobListener.java
└── scraper/
    ├── jobconfig/AiUpdateScraperJobConfig.java
    ├── reader/AiUpdateScraperItemReader.java
    ├── processor/AiUpdateScraperProcessor.java
    ├── writer/AiUpdateScraperWriter.java
    └── listener/ScraperJobListener.java
```

### 4.2 Job 설계 - GitHub Releases

```java
@Configuration
@RequiredArgsConstructor
public class AiUpdateGitHubJobConfig {

    private static final String JOB_NAME = "ai-update.github";
    private static final String STEP_NAME = JOB_NAME + ".step1";
    private static final int CHUNK_SIZE = 50;

    private final GitHubContract githubApi;
    private final AiUpdateInternalContract aiUpdateApi;

    @Bean(name = JOB_NAME)
    public Job aiUpdateGitHubJob(JobRepository jobRepository,
                                  @Qualifier(STEP_NAME) Step step,
                                  @Qualifier(JOB_NAME + ".listener") GitHubJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .listener(listener)
            .build();
    }

    @Bean(name = STEP_NAME)
    @JobScope
    public Step step(JobRepository jobRepository,
                     PlatformTransactionManager transactionManager,
                     @Qualifier(STEP_NAME + ".reader") GitHubReleasePagingItemReader reader,
                     @Qualifier(STEP_NAME + ".processor") GitHubReleaseProcessor processor,
                     @Qualifier(STEP_NAME + ".writer") GitHubReleaseWriter writer) {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<GitHubDto.Release, AiUpdateCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
}
```

### 4.3 Reader 설계 - GitHub Releases

```java
public class GitHubReleasePagingItemReader extends AbstractPagingItemReader<GitHubDto.Release> {

    private final GitHubContract githubApi;
    private final List<RepositoryConfig> repositories;
    private int currentRepoIndex = 0;

    @Override
    protected void doReadPage() {
        if (currentRepoIndex >= repositories.size()) {
            results = Collections.emptyList();
            return;
        }

        RepositoryConfig repo = repositories.get(currentRepoIndex);
        List<GitHubDto.Release> releases = githubApi.getReleases(
            repo.getOwner(),
            repo.getRepo(),
            getPageSize()
        );

        if (releases.isEmpty()) {
            currentRepoIndex++;
            doReadPage();  // 다음 저장소로
            return;
        }

        results = new ArrayList<>(releases);
    }
}
```

### 4.4 Processor 설계

```java
public class GitHubReleaseProcessor implements ItemProcessor<GitHubDto.Release, AiUpdateCreateRequest> {

    private final AiProvider provider;

    @Override
    public AiUpdateCreateRequest process(GitHubDto.Release release) {
        // prerelease, draft 제외
        if (release.prerelease() || release.draft()) {
            return null;
        }

        return AiUpdateCreateRequest.builder()
            .provider(provider)
            .updateType(AiUpdateType.SDK_RELEASE)
            .title(release.name() != null ? release.name() : release.tagName())
            .summary(truncate(release.body(), 500))
            .url(release.htmlUrl())
            .publishedAt(release.publishedAt())
            .sourceType(SourceType.GITHUB_RELEASE)
            .status(PostStatus.DRAFT)
            .externalId("github:" + release.id())
            .metadata(Map.of(
                "version", release.tagName(),
                "github_repo", provider.name().toLowerCase() + "/" + release.repoName()
            ))
            .build();
    }
}
```

### 4.5 Writer 설계

```java
public class GitHubReleaseWriter implements ItemWriter<AiUpdateCreateRequest> {

    private final AiUpdateInternalContract aiUpdateApi;
    private final SlackService slackService;
    private final String apiKey;

    @Override
    public void write(Chunk<? extends AiUpdateCreateRequest> chunk) {
        List<AiUpdateCreateRequest> items = new ArrayList<>(chunk.getItems());

        if (items.isEmpty()) return;

        AiUpdateBatchRequest request = AiUpdateBatchRequest.builder()
            .items(items)
            .build();

        ApiResponse<AiUpdateBatchResponse> response = aiUpdateApi.createBatchInternal(apiKey, request);

        if (!"2000".equals(response.code())) {
            throw new RuntimeException("AI Update batch creation failed: " + response.message());
        }

        // 신규 생성된 항목에 대해 Slack 알림
        for (AiUpdateDetailResponse created : response.data().created()) {
            slackService.sendAiUpdateNotification(created);
        }
    }
}
```

---

## 5. Feign Client 설계

### 5.1 GitHub Releases API 추가

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

### 5.2 GitHub DTO 추가

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

### 5.3 내부 API Contract

```java
public interface AiUpdateInternalContract {

    @PostMapping("/api/v1/ai-update/internal")
    ApiResponse<AiUpdateDetailResponse> createInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody AiUpdateCreateRequest request
    );

    @PostMapping("/api/v1/ai-update/internal/batch")
    ApiResponse<AiUpdateBatchResponse> createBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody AiUpdateBatchRequest request
    );
}
```

---

## 6. API 엔드포인트 설계

### 6.1 엔드포인트 목록

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | /api/v1/ai-update | 목록 조회 | JWT |
| GET | /api/v1/ai-update/{id} | 상세 조회 | JWT |
| GET | /api/v1/ai-update/search | 검색 | JWT |
| POST | /api/v1/ai-update/internal | 단건 생성 | X-Internal-Api-Key |
| POST | /api/v1/ai-update/internal/batch | 다건 생성 | X-Internal-Api-Key |
| POST | /api/v1/ai-update/{id}/approve | 승인 | X-Internal-Api-Key |
| POST | /api/v1/ai-update/{id}/reject | 거부 | X-Internal-Api-Key |

### 6.2 Request/Response DTO

```java
// 목록 조회 요청
public record AiUpdateListRequest(
    @Min(0) int page,
    @Min(1) @Max(100) int size,
    AiProvider provider,      // 필터 (선택)
    AiUpdateType updateType,  // 필터 (선택)
    PostStatus status         // 필터 (선택)
) {}

// 목록 조회 응답
public record AiUpdateListResponse(
    List<AiUpdateSummary> items,
    PageInfo pageInfo
) {}

public record AiUpdateSummary(
    String id,
    String provider,
    String updateType,
    String title,
    String url,
    LocalDateTime publishedAt,
    String status
) {}

// 생성 요청
public record AiUpdateCreateRequest(
    @NotNull AiProvider provider,
    @NotNull AiUpdateType updateType,
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

## 7. Slack 알림 통합

### 7.1 알림 서비스

```java
@Service
@RequiredArgsConstructor
public class AiUpdateSlackService {

    private final SlackClient slackClient;

    @Value("${slack.ai-update.channel}")
    private String channel;

    public void sendAiUpdateNotification(AiUpdateDetailResponse update) {
        String message = buildMessage(update);
        slackClient.sendMessage(channel, message);
    }

    private String buildMessage(AiUpdateDetailResponse update) {
        return String.format("""
            :sparkles: *새로운 AI 업데이트*

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

### 7.2 설정

```yaml
# application.yml
slack:
  ai-update:
    channel: "#ai-updates"
    enabled: true
```

---

## 8. 설정 파일 구조

### 8.1 application-ai-update.yml

```yaml
ai-update:
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

  scraper:
    sources:
      - provider: OPENAI
        url: https://openai.com/blog
        selector: "article"
      - provider: ANTHROPIC
        url: https://www.anthropic.com/news
        selector: "article"
      - provider: GOOGLE
        url: https://blog.google/technology/ai/
        selector: "article"
      - provider: META
        url: https://ai.meta.com/blog/
        selector: "article"

  schedule:
    github:
      cron: "0 0 */6 * * *"  # 6시간마다
    scraper:
      cron: "0 30 */6 * * *"  # 6시간마다 (GitHub 30분 후)

internal-api:
  ai-update:
    api-key: ${AI_UPDATE_INTERNAL_API_KEY:default-ai-update-key}
```

---

## 9. 시퀀스 다이어그램

```
┌──────────┐     ┌───────────┐     ┌──────────────┐     ┌─────────┐     ┌───────┐
│Scheduler │     │ BatchJob  │     │ InternalAPI  │     │MariaDB/ │     │ Slack │
└────┬─────┘     └─────┬─────┘     └──────┬───────┘     │MongoDB  │     └───┬───┘
     │                 │                   │            └────┬────┘         │
     │ trigger job     │                   │                 │              │
     │────────────────>│                   │                 │              │
     │                 │                   │                 │              │
     │                 │ read releases     │                 │              │
     │                 │──────────────────>│                 │              │
     │                 │                   │                 │              │
     │                 │ process & write   │                 │              │
     │                 │──────────────────>│                 │              │
     │                 │                   │                 │              │
     │                 │                   │ save            │              │
     │                 │                   │────────────────>│              │
     │                 │                   │                 │              │
     │                 │                   │ send notification              │
     │                 │                   │────────────────────────────────>│
     │                 │                   │                 │              │
```

---

## 10. 참고 자료

- GitHub REST API - Releases: https://docs.github.com/en/rest/releases/releases
- Spring Batch Reference: https://docs.spring.io/spring-batch/reference/
- Slack API: https://api.slack.com/messaging/sending
