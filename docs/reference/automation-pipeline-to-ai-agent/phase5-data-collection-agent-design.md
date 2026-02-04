# Phase 5: AI Agent 자율 데이터 수집 기능 설계서

## 1. 개요

### 1.1 목적
`api-agent` 모듈의 AI Agent에 자율 데이터 수집(collect) 기능을 추가한다.
현재 Agent는 읽기 전용 Tool만 보유하고 있으나, 배치 잡이 수행하는 데이터 수집 파이프라인을
Agent 내부에서 직접 실행하여 MongoDB에 저장하고, 수집 결과 통계를 채팅에서 보고할 수 있도록 개선한다.

### 1.2 전제 조건
- Phase 1~4 완료: 데이터 수집 파이프라인, LangChain4j Tool 래퍼, Agent 통합, 분석 Tool 재설계
- `api-agent` 모듈에 LangChain4j 1.10.0 + OpenAI GPT-4o-mini 설정 완료
- MongoDB Atlas `emerging_techs` 컬렉션에 데이터 수집 완료
- Internal API(`EmergingTechInternalContract.createEmergingTechBatchInternal`) 정상 동작

### 1.3 변경 요약

| 구분 | 항목 | 설명 |
|------|------|------|
| 신규 Tool | `collect_github_releases` | GitHub 저장소 릴리스 수집 및 DB 저장 |
| 신규 Tool | `collect_rss_feeds` | OpenAI/Google 블로그 RSS 피드 수집 및 DB 저장 |
| 신규 Tool | `collect_scraped_articles` | Anthropic/Meta 블로그 크롤링 및 DB 저장 |
| 신규 파일 | `DataCollectionResultDto.java` | 수집 결과 통계 DTO |
| 신규 파일 | `DataCollectionToolAdapter.java` | 수집 파이프라인 오케스트레이터 |
| 신규 파일 | `DataCollectionProcessorUtil.java` | 배치 Processor 변환 로직 유틸리티 |
| 신규 파일 | `GitHubDataCollectionService.java` | GitHub 릴리스 조회 서비스 |
| 신규 파일 | `RssDataCollectionService.java` | RSS 피드 파싱 서비스 |
| 신규 파일 | `ScraperDataCollectionService.java` | 웹 스크래핑 서비스 |
| 수정 파일 | `EmergingTechAgentTools.java` | 3개 @Tool 메서드 추가 |
| 수정 파일 | `AgentPromptConfig.java` | tools/rules 필드 확장 |
| 수정 파일 | `build.gradle` | `client-rss` 의존성 추가 |
| 수정 파일 | `application-agent-api.yml` | `rss` profile include 추가 |

---

## 2. 아키텍처 설계

### 2.1 데이터 흐름

```
사용자 자연어 입력 (예: "OpenAI 최신 릴리스를 수집해줘")
    |
    v
LLM이 Tool 선택 (예: collect_github_releases)
    |
    v
EmergingTechAgentTools.collectGitHubReleases(owner, repo)
    ├── 입력값 검증 (ToolInputValidator)
    ├── 메트릭 기록 (metrics.incrementToolCall)
    └── DataCollectionToolAdapter.collectGitHubReleases(owner, repo)
        ├── GitHubDataCollectionService.fetchValidReleases(owner, repo, 10)
        │   └── GitHubContract.getReleases() → draft/prerelease 필터링
        ├── DataCollectionProcessorUtil.processGitHubRelease(release, owner, repo, provider)
        │   └── InternalApiDto.EmergingTechCreateRequest 생성
        ├── EmergingTechInternalContract.createEmergingTechBatchInternal(apiKey, batchRequest)
        │   └── EmergingTechBatchResponse 수신 (newCount, duplicateCount 등)
        └── DataCollectionResultDto 구성 및 반환
    |
    v
LLM이 DataCollectionResultDto(JSON)를 해석하여 사용자에게 통계 보고
```

### 2.2 계층 구조

```
EmergingTechAgentTools (Tool Layer)
    └── DataCollectionToolAdapter (Adapter Layer)
        ├── GitHubDataCollectionService (Service Layer)
        │   └── GitHubContract (Client Layer - client-feign)
        ├── RssDataCollectionService (Service Layer)
        │   ├── OpenAiBlogRssParser (Client Layer - client-rss)
        │   └── GoogleAiBlogRssParser (Client Layer - client-rss)
        ├── ScraperDataCollectionService (Service Layer)
        │   ├── AnthropicNewsScraper (Client Layer - client-scraper)
        │   └── MetaAiBlogScraper (Client Layer - client-scraper)
        ├── DataCollectionProcessorUtil (Utility)
        └── EmergingTechInternalContract (Client Layer - client-feign)
```

### 2.3 설계 원칙

- 기존 Adapter 패턴(Tool -> Adapter -> External Service) 준수
- 기존 client 모듈(client-feign, client-rss, client-scraper)을 재사용
- 배치 잡의 Processor 변환 로직을 유틸리티 클래스로 추출하여 Agent에서 공유
- Internal API를 통한 MongoDB 저장 (Agent가 MongoDB에 직접 접근하지 않음)
- LangChain4j @Tool 반환값은 JSON 직렬화 -> LLM에 전달 -> 사용자에게 통계 보고
- SOLID 원칙, 클린코드, 최소한의 한글 주석

---

## 3. 신규 파일 상세 설계

### 3.1 DataCollectionResultDto

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/tool/dto/DataCollectionResultDto.java`

**역할**: 수집 결과 통계 DTO. LangChain4j가 JSON으로 직렬화하여 LLM에 전달하면, LLM이 이를 해석하여 사용자에게 보고한다.

```java
package com.tech.n.ai.api.agent.tool.dto;

import java.util.List;

/**
 * 데이터 수집 결과 통계 DTO
 * LangChain4j @Tool 반환 시 JSON 직렬화되어 LLM에 전달됨
 */
public record DataCollectionResultDto(
    String source,              // "GITHUB_RELEASES", "RSS_FEEDS", "WEB_SCRAPING"
    String provider,            // 필터된 제공자 (빈 문자열이면 "ALL")
    int totalCollected,         // 외부 소스에서 수집된 원시 데이터 건수
    int totalProcessed,         // 유효성 검증 통과 후 Internal API에 전송한 건수
    int newCount,               // DB에 신규 저장된 건수
    int duplicateCount,         // 중복으로 스킵된 건수
    int failureCount,           // 저장 실패 건수
    List<String> failureMessages, // 실패 사유 목록
    String summary              // 사람이 읽을 수 있는 한 줄 요약
) {

    /**
     * 성공 결과 생성 팩토리 메서드
     */
    public static DataCollectionResultDto success(
            String source,
            String provider,
            int totalCollected,
            int totalProcessed,
            int newCount,
            int duplicateCount,
            int failureCount,
            List<String> failureMessages
    ) {
        String resolvedProvider = (provider == null || provider.isBlank()) ? "ALL" : provider;
        String summary = String.format(
            "%s(%s): 수집 %d건, 전송 %d건, 신규 %d건, 중복 %d건, 실패 %d건",
            source, resolvedProvider, totalCollected, totalProcessed,
            newCount, duplicateCount, failureCount
        );
        return new DataCollectionResultDto(
            source, resolvedProvider, totalCollected, totalProcessed,
            newCount, duplicateCount, failureCount, failureMessages, summary
        );
    }

    /**
     * 실패 결과 생성 팩토리 메서드
     */
    public static DataCollectionResultDto failure(
            String source,
            String provider,
            int totalCollected,
            String reason
    ) {
        String resolvedProvider = (provider == null || provider.isBlank()) ? "ALL" : provider;
        String summary = String.format(
            "%s(%s): 수집 %d건, 저장 실패 - %s",
            source, resolvedProvider, totalCollected, reason
        );
        return new DataCollectionResultDto(
            source, resolvedProvider, totalCollected, 0,
            0, 0, 0, List.of(reason), summary
        );
    }
}
```

### 3.2 DataCollectionProcessorUtil

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/tool/util/DataCollectionProcessorUtil.java`

**역할**: 배치 Processor의 핵심 변환 로직을 정적 메서드로 추출한 유틸리티 클래스.
Agent에서는 배치 모듈의 `EmergingTechCreateRequest`(중간 DTO)를 경유하지 않고,
`InternalApiDto.EmergingTechCreateRequest`(Feign DTO)를 직접 생성한다.

**참조**:
- `GitHubReleasesProcessor.java:56-90`
- `EmergingTechRssProcessor.java:42-104`
- `EmergingTechScraperProcessor.java:28-82`
- `AbstractEmergingTechWriter.java:115-138`

```java
package com.tech.n.ai.api.agent.tool.util;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;
import com.tech.n.ai.domain.mongodb.enums.EmergingTechType;
import com.tech.n.ai.domain.mongodb.enums.PostStatus;
import com.tech.n.ai.domain.mongodb.enums.SourceType;
import com.tech.n.ai.domain.mongodb.enums.TechProvider;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * 배치 Processor 변환 로직 추출 유틸리티
 * 배치 모듈의 Processor 변환 로직을 Agent에서 재사용하기 위해 정적 메서드로 제공
 */
@Slf4j
public final class DataCollectionProcessorUtil {

    private static final int SUMMARY_MAX_LENGTH = 500;

    private DataCollectionProcessorUtil() {
    }

    // ========== GitHub Release 변환 ==========

    /**
     * GitHub Release -> InternalApiDto.EmergingTechCreateRequest 변환
     *
     * @param release GitHub 릴리스 정보
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     * @param provider 기술 제공자
     * @return 변환된 요청 DTO, 변환 실패 시 null
     */
    public static InternalApiDto.EmergingTechCreateRequest processGitHubRelease(
            GitHubDto.Release release, String owner, String repo, TechProvider provider) {
        try {
            String title = (release.name() != null && !release.name().isBlank())
                ? release.name() : release.tagName();

            String author = release.author() != null ? release.author().login() : null;

            InternalApiDto.EmergingTechMetadataRequest metadata =
                InternalApiDto.EmergingTechMetadataRequest.builder()
                    .version(release.tagName())
                    .tags(List.of("sdk", "release"))
                    .author(author)
                    .githubRepo(owner + "/" + repo)
                    .build();

            return InternalApiDto.EmergingTechCreateRequest.builder()
                .provider(provider.name())
                .updateType(EmergingTechType.SDK_RELEASE.name())
                .title(title)
                .summary(TextTruncator.truncate(release.body(), SUMMARY_MAX_LENGTH))
                .url(release.htmlUrl())
                .publishedAt(parsePublishedAt(release.publishedAt()))
                .sourceType(SourceType.GITHUB_RELEASE.name())
                .status(PostStatus.DRAFT.name())
                .externalId("github:" + release.id())
                .metadata(metadata)
                .build();
        } catch (Exception e) {
            log.warn("GitHub release 변환 실패: {}", release.tagName(), e);
            return null;
        }
    }

    // ========== RSS Feed 변환 ==========

    /**
     * RssFeedItem -> InternalApiDto.EmergingTechCreateRequest 변환
     *
     * @param item RSS 피드 아이템
     * @return 변환된 요청 DTO, 변환 실패 시 null
     */
    public static InternalApiDto.EmergingTechCreateRequest processRssFeedItem(RssFeedItem item) {
        try {
            if (item.title() == null || item.link() == null) {
                log.debug("RSS item 스킵: title 또는 link가 null");
                return null;
            }

            if (item.publishedDate() != null
                    && item.publishedDate().isBefore(LocalDateTime.now().minusMonths(1))) {
                log.debug("RSS item 스킵: 1개월 이전 항목 - title={}", item.title());
                return null;
            }

            TechProvider provider = resolveRssProvider(item.link());

            InternalApiDto.EmergingTechMetadataRequest metadata =
                InternalApiDto.EmergingTechMetadataRequest.builder()
                    .tags(extractTags(item.category()))
                    .author(Objects.requireNonNullElse(item.author(), ""))
                    .build();

            return InternalApiDto.EmergingTechCreateRequest.builder()
                .provider(provider.name())
                .updateType(classifyUpdateType(item.title(), item.category()).name())
                .title(item.title())
                .summary(truncateHtml(item.description(), SUMMARY_MAX_LENGTH))
                .url(item.link())
                .publishedAt(item.publishedDate())
                .sourceType(SourceType.RSS.name())
                .status(PostStatus.DRAFT.name())
                .externalId("rss:" + generateHash(
                    item.guid() != null ? item.guid() : item.link()))
                .metadata(metadata)
                .build();
        } catch (Exception e) {
            log.warn("RSS feed item 변환 실패: {}", item.title(), e);
            return null;
        }
    }

    // ========== Scraped Article 변환 ==========

    /**
     * ScrapedTechArticle -> InternalApiDto.EmergingTechCreateRequest 변환
     *
     * @param article 스크래핑된 기사
     * @return 변환된 요청 DTO, 변환 실패 시 null
     */
    public static InternalApiDto.EmergingTechCreateRequest processScrapedArticle(
            ScrapedTechArticle article) {
        try {
            if (article.title() == null || article.url() == null) {
                log.debug("Scraped article 스킵: title 또는 url이 null");
                return null;
            }

            InternalApiDto.EmergingTechMetadataRequest metadata =
                InternalApiDto.EmergingTechMetadataRequest.builder()
                    .author(Objects.requireNonNullElse(article.author(), ""))
                    .tags(extractTags(article.category()))
                    .build();

            return InternalApiDto.EmergingTechCreateRequest.builder()
                .provider(article.providerName())
                .updateType(classifyUpdateType(article.title(), article.category()).name())
                .title(article.title())
                .summary(TextTruncator.truncate(article.summary(), SUMMARY_MAX_LENGTH))
                .url(article.url())
                .publishedAt(article.publishedDate())
                .sourceType(SourceType.WEB_SCRAPING.name())
                .status(PostStatus.DRAFT.name())
                .externalId("scraper:" + generateHash(article.url()))
                .metadata(metadata)
                .build();
        } catch (Exception e) {
            log.warn("Scraped article 변환 실패: {}", article.title(), e);
            return null;
        }
    }

    // ========== 공통 유틸리티 메서드 ==========

    /**
     * RSS URL 기반 Provider 매핑
     */
    static TechProvider resolveRssProvider(String url) {
        if (url.contains("openai.com")) return TechProvider.OPENAI;
        if (url.contains("blog.google")) return TechProvider.GOOGLE;
        throw new IllegalArgumentException("Unknown RSS source URL: " + url);
    }

    /**
     * 키워드 기반 UpdateType 분류
     */
    static EmergingTechType classifyUpdateType(String title, String category) {
        String lowerTitle = title.toLowerCase();
        String lowerCategory = category != null ? category.toLowerCase() : "";

        if (lowerTitle.contains("api") || lowerCategory.contains("api"))
            return EmergingTechType.API_UPDATE;
        if (lowerTitle.contains("release") || lowerTitle.contains("introducing")
                || lowerTitle.contains("model") || lowerTitle.contains("announcing"))
            return EmergingTechType.MODEL_RELEASE;
        if (lowerTitle.contains("launch") || lowerTitle.contains("product")
                || lowerTitle.contains("available") || lowerTitle.contains("new service"))
            return EmergingTechType.PRODUCT_LAUNCH;
        if (lowerTitle.contains("platform") || lowerTitle.contains("cloud")
                || lowerTitle.contains("infrastructure") || lowerTitle.contains("update")
                || lowerCategory.contains("platform"))
            return EmergingTechType.PLATFORM_UPDATE;
        return EmergingTechType.BLOG_POST;
    }

    /**
     * ISO 8601 날짜 파싱 (실패 시 now())
     */
    static LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("publishedAt 파싱 실패: {}", publishedAt);
            return LocalDateTime.now();
        }
    }

    /**
     * HTML 태그 제거 후 절단
     */
    static String truncateHtml(String text, int maxLength) {
        if (text == null) return "";
        String cleaned = text.replaceAll("<[^>]+>", "").trim();
        if (cleaned.length() <= maxLength) return cleaned;
        return cleaned.substring(0, maxLength) + "...";
    }

    /**
     * SHA-256 해시 생성 (앞 16자리)
     */
    static String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * 카테고리 문자열 -> 태그 리스트 변환
     */
    static List<String> extractTags(String category) {
        if (category == null || category.isBlank()) return List.of();
        return List.of(category.split(",")).stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
```

### 3.3 GitHubDataCollectionService

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/service/GitHubDataCollectionService.java`

**역할**: GitHubContract를 사용한 릴리스 조회 + draft/prerelease 필터링 서비스.
기존 `GitHubToolAdapter`의 조회 로직과 유사하나, 필터링된 `GitHubDto.Release` 원본 객체를 반환하여
`DataCollectionProcessorUtil`에서 `InternalApiDto.EmergingTechCreateRequest`로 변환할 수 있도록 한다.

```java
package com.tech.n.ai.api.agent.service;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent용 GitHub 릴리스 수집 서비스
 * GitHubContract를 통해 릴리스를 조회하고 draft/prerelease를 필터링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubDataCollectionService {

    private final GitHubContract githubContract;

    /**
     * GitHub 저장소의 유효한 릴리스 조회
     *
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     * @param perPage 조회 건수
     * @return draft/prerelease가 제외된 릴리스 목록
     */
    public List<GitHubDto.Release> fetchValidReleases(String owner, String repo, int perPage) {
        try {
            GitHubDto.ReleasesRequest request = GitHubDto.ReleasesRequest.builder()
                .owner(owner)
                .repo(repo)
                .perPage(perPage)
                .page(1)
                .build();

            GitHubDto.ReleasesResponse response = githubContract.getReleases(request);

            if (response == null || response.releases() == null) {
                log.warn("GitHub releases 응답이 null: owner={}, repo={}", owner, repo);
                return List.of();
            }

            List<GitHubDto.Release> validReleases = response.releases().stream()
                .filter(r -> !Boolean.TRUE.equals(r.prerelease()))
                .filter(r -> !Boolean.TRUE.equals(r.draft()))
                .toList();

            log.info("GitHub releases 조회 완료: owner={}, repo={}, total={}, valid={}",
                owner, repo, response.releases().size(), validReleases.size());

            return validReleases;
        } catch (Exception e) {
            log.error("GitHub releases 조회 실패: owner={}, repo={}", owner, repo, e);
            return List.of();
        }
    }
}
```

### 3.4 RssDataCollectionService

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/service/RssDataCollectionService.java`

**역할**: OpenAiBlogRssParser, GoogleAiBlogRssParser 빈을 주입받아 parse() 메서드를 호출하는 래퍼.
제공자별 필터링 로직을 추가하여 `List<RssFeedItem>`을 반환한다.

```java
package com.tech.n.ai.api.agent.service;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.parser.GoogleAiBlogRssParser;
import com.tech.n.ai.client.rss.parser.OpenAiBlogRssParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent용 RSS 피드 수집 서비스
 * RSS 파서를 통해 피드를 수집하고 제공자별 필터링 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssDataCollectionService {

    private final OpenAiBlogRssParser openAiBlogRssParser;
    private final GoogleAiBlogRssParser googleAiBlogRssParser;

    /**
     * RSS 피드 수집
     *
     * @param provider 제공자 필터 ("OPENAI", "GOOGLE", 빈 문자열=전체)
     * @return 수집된 RSS 피드 아이템 목록
     */
    public List<RssFeedItem> fetchRssFeeds(String provider) {
        List<RssFeedItem> allItems = new ArrayList<>();

        boolean fetchAll = provider == null || provider.isBlank();

        if (fetchAll || "OPENAI".equalsIgnoreCase(provider)) {
            allItems.addAll(parseOpenAi());
        }

        if (fetchAll || "GOOGLE".equalsIgnoreCase(provider)) {
            allItems.addAll(parseGoogle());
        }

        log.info("RSS 피드 수집 완료: provider={}, total={}", provider, allItems.size());
        return allItems;
    }

    private List<RssFeedItem> parseOpenAi() {
        try {
            List<RssFeedItem> items = openAiBlogRssParser.parse();
            log.info("OpenAI RSS 파싱 완료: {} items", items.size());
            return items;
        } catch (Exception e) {
            log.error("OpenAI RSS 파싱 실패", e);
            return List.of();
        }
    }

    private List<RssFeedItem> parseGoogle() {
        try {
            List<RssFeedItem> items = googleAiBlogRssParser.parse();
            log.info("Google AI RSS 파싱 완료: {} items", items.size());
            return items;
        } catch (Exception e) {
            log.error("Google AI RSS 파싱 실패", e);
            return List.of();
        }
    }
}
```

### 3.5 ScraperDataCollectionService

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/service/ScraperDataCollectionService.java`

**역할**: AnthropicNewsScraper, MetaAiBlogScraper 빈을 주입받아 scrapeArticles() 메서드를 호출하는 래퍼.
제공자별 필터링 로직을 추가하여 `List<ScrapedTechArticle>`을 반환한다.

```java
package com.tech.n.ai.api.agent.service;

import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;
import com.tech.n.ai.client.scraper.scraper.AnthropicNewsScraper;
import com.tech.n.ai.client.scraper.scraper.MetaAiBlogScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent용 웹 스크래핑 수집 서비스
 * 기술 블로그 스크래퍼를 통해 기사를 수집하고 제공자별 필터링 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperDataCollectionService {

    private final AnthropicNewsScraper anthropicNewsScraper;
    private final MetaAiBlogScraper metaAiBlogScraper;

    /**
     * 기술 블로그 스크래핑
     *
     * @param provider 제공자 필터 ("ANTHROPIC", "META", 빈 문자열=전체)
     * @return 스크래핑된 기사 목록
     */
    public List<ScrapedTechArticle> scrapeArticles(String provider) {
        List<ScrapedTechArticle> allArticles = new ArrayList<>();

        boolean fetchAll = provider == null || provider.isBlank();

        if (fetchAll || "ANTHROPIC".equalsIgnoreCase(provider)) {
            allArticles.addAll(scrapeAnthropic());
        }

        if (fetchAll || "META".equalsIgnoreCase(provider)) {
            allArticles.addAll(scrapeMeta());
        }

        log.info("웹 스크래핑 완료: provider={}, total={}", provider, allArticles.size());
        return allArticles;
    }

    private List<ScrapedTechArticle> scrapeAnthropic() {
        try {
            List<ScrapedTechArticle> articles = anthropicNewsScraper.scrapeArticles();
            log.info("Anthropic 스크래핑 완료: {} articles", articles.size());
            return articles;
        } catch (Exception e) {
            log.error("Anthropic 스크래핑 실패", e);
            return List.of();
        }
    }

    private List<ScrapedTechArticle> scrapeMeta() {
        try {
            List<ScrapedTechArticle> articles = metaAiBlogScraper.scrapeArticles();
            log.info("Meta AI 스크래핑 완료: {} articles", articles.size());
            return articles;
        } catch (Exception e) {
            log.error("Meta AI 스크래핑 실패", e);
            return List.of();
        }
    }
}
```

### 3.6 DataCollectionToolAdapter

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/tool/adapter/DataCollectionToolAdapter.java`

**역할**: 수집 파이프라인 오케스트레이터. fetch -> process -> Internal API 배치 호출 -> 결과 통계 반환.

```java
package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.service.GitHubDataCollectionService;
import com.tech.n.ai.api.agent.service.RssDataCollectionService;
import com.tech.n.ai.api.agent.service.ScraperDataCollectionService;
import com.tech.n.ai.api.agent.tool.dto.DataCollectionResultDto;
import com.tech.n.ai.api.agent.tool.util.DataCollectionProcessorUtil;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.domain.mongodb.enums.TechProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 데이터 수집 파이프라인 오케스트레이터
 * fetch -> process -> Internal API 배치 호출 -> 결과 통계 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCollectionToolAdapter {

    private static final Map<String, TechProvider> OWNER_PROVIDER_MAP = Map.of(
        "openai", TechProvider.OPENAI,
        "anthropics", TechProvider.ANTHROPIC,
        "google", TechProvider.GOOGLE,
        "google-deepmind", TechProvider.GOOGLE,
        "meta-llama", TechProvider.META,
        "facebookresearch", TechProvider.META,
        "xai-org", TechProvider.XAI
    );

    private final GitHubDataCollectionService githubService;
    private final RssDataCollectionService rssService;
    private final ScraperDataCollectionService scraperService;
    private final EmergingTechInternalContract emergingTechInternalApi;

    @Value("${internal-api.emerging-tech.api-key}")
    private String apiKey;

    /**
     * GitHub 릴리스 수집 및 DB 저장
     *
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     * @return 수집 결과 통계
     */
    public DataCollectionResultDto collectGitHubReleases(String owner, String repo) {
        String source = "GITHUB_RELEASES";
        TechProvider provider = OWNER_PROVIDER_MAP.getOrDefault(
            owner.toLowerCase(), TechProvider.OPENAI);

        // 1. fetch
        List<GitHubDto.Release> releases = githubService.fetchValidReleases(owner, repo, 10);
        int totalCollected = releases.size();

        if (releases.isEmpty()) {
            return DataCollectionResultDto.success(
                source, provider.name(), 0, 0, 0, 0, 0, List.of());
        }

        // 2. process
        List<InternalApiDto.EmergingTechCreateRequest> requests = releases.stream()
            .map(release -> DataCollectionProcessorUtil.processGitHubRelease(
                release, owner, repo, provider))
            .filter(Objects::nonNull)
            .toList();

        // 3. Internal API 호출
        return sendBatchAndBuildResult(source, provider.name(), totalCollected, requests);
    }

    /**
     * RSS 피드 수집 및 DB 저장
     *
     * @param provider 제공자 필터 (OPENAI, GOOGLE, 빈 문자열=전체)
     * @return 수집 결과 통계
     */
    public DataCollectionResultDto collectRssFeeds(String provider) {
        String source = "RSS_FEEDS";

        // 1. fetch
        List<RssFeedItem> items = rssService.fetchRssFeeds(provider);
        int totalCollected = items.size();

        if (items.isEmpty()) {
            return DataCollectionResultDto.success(
                source, provider, 0, 0, 0, 0, 0, List.of());
        }

        // 2. process
        List<InternalApiDto.EmergingTechCreateRequest> requests = items.stream()
            .map(DataCollectionProcessorUtil::processRssFeedItem)
            .filter(Objects::nonNull)
            .toList();

        // 3. Internal API 호출
        return sendBatchAndBuildResult(source, provider, totalCollected, requests);
    }

    /**
     * 웹 스크래핑 수집 및 DB 저장
     *
     * @param provider 제공자 필터 (ANTHROPIC, META, 빈 문자열=전체)
     * @return 수집 결과 통계
     */
    public DataCollectionResultDto collectScrapedArticles(String provider) {
        String source = "WEB_SCRAPING";

        // 1. fetch
        List<ScrapedTechArticle> articles = scraperService.scrapeArticles(provider);
        int totalCollected = articles.size();

        if (articles.isEmpty()) {
            return DataCollectionResultDto.success(
                source, provider, 0, 0, 0, 0, 0, List.of());
        }

        // 2. process
        List<InternalApiDto.EmergingTechCreateRequest> requests = articles.stream()
            .map(DataCollectionProcessorUtil::processScrapedArticle)
            .filter(Objects::nonNull)
            .toList();

        // 3. Internal API 호출
        return sendBatchAndBuildResult(source, provider, totalCollected, requests);
    }

    /**
     * Internal API 배치 호출 및 결과 통계 구성
     */
    private DataCollectionResultDto sendBatchAndBuildResult(
            String source, String provider, int totalCollected,
            List<InternalApiDto.EmergingTechCreateRequest> requests) {

        if (requests.isEmpty()) {
            return DataCollectionResultDto.success(
                source, provider, totalCollected, 0, 0, 0, 0, List.of());
        }

        try {
            InternalApiDto.EmergingTechBatchRequest batchRequest =
                InternalApiDto.EmergingTechBatchRequest.builder()
                    .items(requests)
                    .build();

            ApiResponse<InternalApiDto.EmergingTechBatchResponse> response =
                emergingTechInternalApi.createEmergingTechBatchInternal(apiKey, batchRequest);

            if (response == null || response.data() == null) {
                return DataCollectionResultDto.failure(
                    source, provider, totalCollected, "Internal API 응답이 null");
            }

            InternalApiDto.EmergingTechBatchResponse data = response.data();

            log.info("{} 수집 결과: total={}, new={}, duplicate={}, failure={}",
                source, data.getTotalCount(), data.getNewCount(),
                data.getDuplicateCount(), data.getFailureCount());

            return DataCollectionResultDto.success(
                source, provider, totalCollected, requests.size(),
                data.getNewCount(), data.getDuplicateCount(),
                data.getFailureCount(), data.getFailureMessages() != null
                    ? data.getFailureMessages() : List.of());
        } catch (Exception e) {
            log.error("{} Internal API 호출 실패", source, e);
            return DataCollectionResultDto.failure(
                source, provider, totalCollected,
                "Internal API 호출 실패: " + e.getMessage());
        }
    }
}
```

---

## 4. 수정 파일 상세 설계

### 4.1 EmergingTechAgentTools 변경

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/tool/EmergingTechAgentTools.java`

**변경 내용**: DataCollectionToolAdapter 주입 + 3개 @Tool 메서드 추가

#### 추가할 import 문

```java
import com.tech.n.ai.api.agent.tool.adapter.DataCollectionToolAdapter;
import com.tech.n.ai.api.agent.tool.dto.DataCollectionResultDto;
```

#### 추가할 필드

```java
private final DataCollectionToolAdapter dataCollectionAdapter;
```

#### 추가할 @Tool 메서드 (3개)

기존 `sendSlackNotification()` 메서드 아래에 추가:

```java
    // ========== 데이터 수집 Tool ==========

    /**
     * GitHub 저장소 릴리스를 수집하여 DB에 저장
     */
    @Tool(name = "collect_github_releases",
          value = "GitHub 저장소의 릴리스를 수집하여 DB에 저장합니다. "
                + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
    public DataCollectionResultDto collectGitHubReleases(
            @P("저장소 소유자 (예: openai, anthropics, google, meta-llama)") String owner,
            @P("저장소 이름 (예: openai-python, anthropic-sdk-python)") String repo
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: collect_github_releases(owner={}, repo={})", owner, repo);

        String validationError = ToolInputValidator.validateGitHubRepo(owner, repo);
        if (validationError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", validationError);
            return DataCollectionResultDto.failure("GITHUB_RELEASES", "", 0, validationError);
        }

        return dataCollectionAdapter.collectGitHubReleases(owner, repo);
    }

    /**
     * OpenAI/Google AI 블로그 RSS 피드를 수집하여 DB에 저장
     */
    @Tool(name = "collect_rss_feeds",
          value = "OpenAI/Google AI 블로그 RSS 피드를 수집하여 DB에 저장합니다. "
                + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
    public DataCollectionResultDto collectRssFeeds(
            @P("제공자 필터 (OPENAI, GOOGLE 또는 빈 문자열=전체 수집)") String provider
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: collect_rss_feeds(provider={})", provider);

        if (provider != null && !provider.isBlank()
                && !"OPENAI".equalsIgnoreCase(provider)
                && !"GOOGLE".equalsIgnoreCase(provider)) {
            metrics().incrementValidationError();
            String error = "Error: provider는 OPENAI, GOOGLE 또는 빈 문자열이어야 합니다 (입력값: " + provider + ")";
            log.warn("Tool 입력값 검증 실패: {}", error);
            return DataCollectionResultDto.failure("RSS_FEEDS", provider, 0, error);
        }

        return dataCollectionAdapter.collectRssFeeds(provider);
    }

    /**
     * Anthropic/Meta AI 기술 블로그를 크롤링하여 DB에 저장
     */
    @Tool(name = "collect_scraped_articles",
          value = "Anthropic/Meta AI 기술 블로그를 크롤링하여 DB에 저장합니다. "
                + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
    public DataCollectionResultDto collectScrapedArticles(
            @P("제공자 필터 (ANTHROPIC, META 또는 빈 문자열=전체 수집)") String provider
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: collect_scraped_articles(provider={})", provider);

        if (provider != null && !provider.isBlank()
                && !"ANTHROPIC".equalsIgnoreCase(provider)
                && !"META".equalsIgnoreCase(provider)) {
            metrics().incrementValidationError();
            String error = "Error: provider는 ANTHROPIC, META 또는 빈 문자열이어야 합니다 (입력값: " + provider + ")";
            log.warn("Tool 입력값 검증 실패: {}", error);
            return DataCollectionResultDto.failure("WEB_SCRAPING", provider, 0, error);
        }

        return dataCollectionAdapter.collectScrapedArticles(provider);
    }
```

### 4.2 AgentPromptConfig 변경

**경로**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/config/AgentPromptConfig.java`

#### tools 필드 변경

```java
    private String tools = """
        - fetch_github_releases: GitHub 저장소 릴리스 조회
        - scrape_web_page: 웹 페이지 크롤링
        - search_emerging_techs: 기존 업데이트 검색
        - get_emerging_tech_statistics: Provider/SourceType/기간별 통계 집계
        - analyze_text_frequency: 키워드 빈도 분석 (Word Cloud)
        - send_slack_notification: Slack 알림 전송
        - collect_github_releases: GitHub 저장소 릴리스 수집 및 DB 저장 (결과 통계 포함)
        - collect_rss_feeds: OpenAI/Google 블로그 RSS 피드 수집 및 DB 저장 (결과 통계 포함)
        - collect_scraped_articles: Anthropic/Meta 블로그 크롤링 및 DB 저장 (결과 통계 포함)""";
```

#### rules 필드 변경

```java
    private String rules = """
        1. 통계 요청 시 get_emerging_tech_statistics로 데이터를 집계하고, Markdown 표와 Mermaid 차트로 보기 쉽게 정리
        2. 키워드 분석 요청 시 analyze_text_frequency로 빈도를 집계하고, Mermaid 차트와 해석을 함께 제공
        3. 데이터 수집 요청 시 fetch_github_releases, scrape_web_page 활용
        4. 중복 확인은 search_emerging_techs 사용
        5. 결과 공유 시 send_slack_notification 활용
        6. 작업 완료 후 결과 요약 제공
        7. 데이터 수집 및 저장 요청 시 collect_* 도구를 사용하여 자동으로 DB에 저장하고, 결과 통계를 보고
        8. 전체 소스 수집 요청 시: collect_github_releases(각 저장소별) → collect_rss_feeds("") → collect_scraped_articles("") 순서로 실행
        9. 수집 결과의 신규/중복/실패 건수를 Markdown 표로 정리하여 제공""";
```

### 4.3 build.gradle 변경

**경로**: `api/agent/build.gradle`

**변경 내용**: `implementation project(':client-rss')` 의존성 추가

```gradle
dependencies {
    // 프로젝트 모듈 의존성
    implementation project(':common-core')
    implementation project(':common-exception')
    implementation project(':domain-mongodb')

    // langchain4j Core + OpenAI (1.10.0: Tool Error Handler 지원)
    implementation 'dev.langchain4j:langchain4j:1.10.0'
    implementation 'dev.langchain4j:langchain4j-open-ai:1.10.0'

    // Agent Tool용 클라이언트 모듈
    implementation project(':client-feign')
    implementation project(':client-slack')
    implementation project(':client-scraper')
    implementation project(':client-rss')    // RSS 피드 수집용 추가

    // Jsoup (ScraperToolAdapter용 HTML 파싱)
    implementation 'org.jsoup:jsoup:1.17.2'

    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
}
```

### 4.4 application-agent-api.yml 변경

**경로**: `api/agent/src/main/resources/application-agent-api.yml`

**변경 내용**: `spring.profiles.include`에 `rss` 프로필을 추가하여 RSS 파서가 필요로 하는 설정(WebClient, RssProperties 등)이 로드되도록 한다.

Agent 모듈의 `application.yml`(메인 프로필 설정 파일)에서 `rss` 프로필을 include에 추가한다.

> **확인 필요**: `application.yml`의 `spring.profiles.include` 목록에 `rss`를 추가. `client-rss` 모듈의 `application-rss.yml`이 자동으로 로드되어 `RssProperties`, `WebClient.Builder` 등의 빈이 등록된다.

---

## 5. 에러 처리 전략

| 계층 | 에러 유형 | 처리 방식 |
|------|----------|----------|
| Service | 외부 API 호출 실패 (FeignException, RssParsingException, ScrapingException 등) | 예외 catch -> 빈 리스트 반환 -> 다른 소스 계속 진행 |
| ProcessorUtil | 변환 실패 | null 반환 -> 해당 아이템 스킵 -> 로그 기록 |
| Adapter | Internal API 호출 실패 | FeignException catch -> DataCollectionResultDto.failure() 반환 |
| Tool | 입력값 검증 실패 | DataCollectionResultDto.failure() 즉시 반환 |

### 5.1 에러 흐름 다이어그램

```
외부 API 호출 실패
    └── Service 계층에서 catch → 빈 리스트 반환
        └── Adapter: 빈 리스트 → success(totalCollected=0)

변환 실패 (단건)
    └── ProcessorUtil에서 null 반환
        └── filter(Objects::nonNull)로 스킵
            └── 다른 아이템은 정상 진행

Internal API 호출 실패
    └── Adapter에서 catch
        └── DataCollectionResultDto.failure(reason=에러 메시지)

입력값 검증 실패
    └── Tool에서 즉시 반환
        └── DataCollectionResultDto.failure(reason=검증 에러)
```

### 5.2 부분 실패 허용

Internal API(`createEmergingTechBatchInternal`)는 자체적으로 부분 실패를 허용한다.
`EmergingTechBatchResponse`의 `failureCount > 0`이어도 `successCount > 0`이면 정상 응답으로 처리한다.
`DataCollectionResultDto`에 `failureCount`와 `failureMessages`를 포함하여 LLM이 사용자에게 보고할 수 있도록 한다.

---

## 6. Tool 정의 상세

### 6.1 collect_github_releases

```java
@Tool(name = "collect_github_releases",
      value = "GitHub 저장소의 릴리스를 수집하여 DB에 저장합니다. "
            + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
public DataCollectionResultDto collectGitHubReleases(
    @P("저장소 소유자 (예: openai, anthropics, google, meta-llama)") String owner,
    @P("저장소 이름 (예: openai-python, anthropic-sdk-python)") String repo
)
```

**검증**: owner/repo 정규식 검증 (기존 `ToolInputValidator.validateGitHubRepo` 재사용)

**Provider 매핑** (EmergingTechGitHubJobConfig.java:51-61 참조):

| Owner | TechProvider |
|-------|-------------|
| openai | OPENAI |
| anthropics | ANTHROPIC |
| google | GOOGLE |
| google-deepmind | GOOGLE |
| meta-llama | META |
| facebookresearch | META |
| xai-org | XAI |

### 6.2 collect_rss_feeds

```java
@Tool(name = "collect_rss_feeds",
      value = "OpenAI/Google AI 블로그 RSS 피드를 수집하여 DB에 저장합니다. "
            + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
public DataCollectionResultDto collectRssFeeds(
    @P("제공자 필터 (OPENAI, GOOGLE 또는 빈 문자열=전체 수집)") String provider
)
```

**검증**: provider가 비어있거나 OPENAI/GOOGLE 중 하나인지 확인

### 6.3 collect_scraped_articles

```java
@Tool(name = "collect_scraped_articles",
      value = "Anthropic/Meta AI 기술 블로그를 크롤링하여 DB에 저장합니다. "
            + "수집 결과(신규/중복/실패 건수)를 반환합니다.")
public DataCollectionResultDto collectScrapedArticles(
    @P("제공자 필터 (ANTHROPIC, META 또는 빈 문자열=전체 수집)") String provider
)
```

**검증**: provider가 비어있거나 ANTHROPIC/META 중 하나인지 확인. XAI는 현재 비활성 상태이므로 제외.

---

## 7. DataCollectionProcessorUtil 변환 로직 상세

각 배치 Processor의 핵심 로직을 정적 메서드로 추출한다.
Agent 모듈에서는 배치 모듈의 `EmergingTechCreateRequest`(중간 DTO)를 경유하지 않고,
`InternalApiDto.EmergingTechCreateRequest`(Feign DTO)를 직접 생성한다.

### 7.1 GitHub Release 변환

**참조**: `GitHubReleasesProcessor.java:56-90`

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

### 7.2 RSS Feed 변환

**참조**: `EmergingTechRssProcessor.java:42-104`

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
  - metadata: tags=item.categories(), author=item.author()
```

### 7.3 Scraped Article 변환

**참조**: `EmergingTechScraperProcessor.java:28-82`

```
입력: ScrapedTechArticle
출력: InternalApiDto.EmergingTechCreateRequest

변환:
  - provider: article.providerName()
  - updateType: 키워드 분류 (RSS와 동일 로직)
  - title: article.title()
  - summary: truncate(article.summary(), 500)
  - url: article.url()
  - publishedAt: article.publishedDate()
  - sourceType: WEB_SCRAPING
  - status: DRAFT
  - externalId: "scraper:" + SHA256(article.url()).substring(0,16)
  - metadata: tags=article.categories(), author=article.author()
```

---

## 8. 파일 구조 변경 요약

### 8.1 신규 파일

```
api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/
├── service/
│   ├── GitHubDataCollectionService.java      (3.3절)
│   ├── RssDataCollectionService.java         (3.4절)
│   └── ScraperDataCollectionService.java     (3.5절)
└── tool/
    ├── adapter/
    │   └── DataCollectionToolAdapter.java    (3.6절)
    ├── dto/
    │   └── DataCollectionResultDto.java      (3.1절)
    └── util/
        └── DataCollectionProcessorUtil.java  (3.2절)
```

### 8.2 수정 파일

```
api/agent/
├── build.gradle                              (4.3절)
├── src/main/java/.../tool/
│   └── EmergingTechAgentTools.java           (4.1절)
├── src/main/java/.../config/
│   └── AgentPromptConfig.java                (4.2절)
└── src/main/resources/
    └── application-agent-api.yml             (4.4절)
```

---

## 9. 구현 순서

| 단계 | 작업 | 의존 관계 |
|------|------|----------|
| 1 | `DataCollectionResultDto` 생성 | 없음 |
| 2 | `DataCollectionProcessorUtil` 생성 | 기존 `TextTruncator` 사용 |
| 3 | `GitHubDataCollectionService` 생성 | 기존 `GitHubContract` 사용 |
| 4 | `RssDataCollectionService` 생성 | 기존 `OpenAiBlogRssParser`, `GoogleAiBlogRssParser` 사용 |
| 5 | `ScraperDataCollectionService` 생성 | 기존 `AnthropicNewsScraper`, `MetaAiBlogScraper` 사용 |
| 6 | `DataCollectionToolAdapter` 생성 | 단계 2~5 완료 필요 |
| 7 | `EmergingTechAgentTools` 수정 | 단계 6 완료 필요 |
| 8 | `AgentPromptConfig` 수정 | 없음 |
| 9 | `build.gradle` 수정 | 없음 |
| 10 | `application-agent-api.yml` 수정 | 없음 |
| 11 | 빌드 확인 및 통합 테스트 | 전체 완료 |

---

## 10. 참고 자료

### 핵심 참조 파일

| 파일 | 참조 사유 |
|------|----------|
| `api/agent/.../tool/EmergingTechAgentTools.java` | 현재 Tool 정의, 패턴 준수 |
| `api/agent/.../tool/adapter/GitHubToolAdapter.java` | Adapter 패턴 참조 |
| `api/agent/.../config/AgentPromptConfig.java` | 시스템 프롬프트 구성 |
| `api/agent/.../tool/validation/ToolInputValidator.java` | 입력 검증 패턴 |
| `batch/source/.../github/processor/GitHubReleasesProcessor.java` | GitHub 변환 로직 원본 |
| `batch/source/.../rss/processor/EmergingTechRssProcessor.java` | RSS 변환 로직 원본 |
| `batch/source/.../scraper/processor/EmergingTechScraperProcessor.java` | Scraper 변환 로직 원본 |
| `batch/source/.../writer/AbstractEmergingTechWriter.java` | Internal API 호출 패턴 |
| `client/feign/.../internal/contract/InternalApiDto.java` | 배치 요청/응답 DTO |
| `client/feign/.../internal/contract/EmergingTechInternalContract.java` | Internal API 계약 |

### LangChain4j @Tool 참고

- **반환 타입**: record/DTO는 JSON 직렬화되어 LLM에 전달됨
- **에러 처리**: 예외 발생 시 `e.getMessage()`가 LLM에 전달됨. 본 설계에서는 예외 대신 `DataCollectionResultDto.failure()`를 반환하여 구조화된 에러 정보 제공
- **입력 검증**: LLM hallucination 방어를 위해 모든 입력값을 사전 검증
