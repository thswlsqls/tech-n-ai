package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.service.GitHubDataCollectionService;
import com.tech.n.ai.api.agent.service.RssDataCollectionService;
import com.tech.n.ai.api.agent.service.ScraperDataCollectionService;
import com.tech.n.ai.api.agent.tool.dto.DataCollectionResultDto;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;
import com.tech.n.ai.common.core.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * DataCollectionToolAdapter 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataCollectionToolAdapter 단위 테스트")
class DataCollectionToolAdapterTest {

    @Mock
    private GitHubDataCollectionService githubService;

    @Mock
    private RssDataCollectionService rssService;

    @Mock
    private ScraperDataCollectionService scraperService;

    @Mock
    private EmergingTechInternalContract emergingTechInternalApi;

    private DataCollectionToolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DataCollectionToolAdapter(
                githubService, rssService, scraperService, emergingTechInternalApi);
        ReflectionTestUtils.setField(adapter, "apiKey", "test-api-key");
    }

    // ========== collectGitHubReleases 테스트 ==========

    @Nested
    @DisplayName("collectGitHubReleases")
    class CollectGitHubReleases {

        @Test
        @DisplayName("정상 수집 및 저장 - 통계 반환")
        void collectGitHubReleases_정상수집() {
            // Given
            List<GitHubDto.Release> releases = List.of(
                    createRelease("v1.0.0", "Release 1", "notes1"),
                    createRelease("v1.1.0", "Release 2", "notes2")
            );

            InternalApiDto.EmergingTechBatchResponse batchResponse =
                    InternalApiDto.EmergingTechBatchResponse.builder()
                            .totalCount(2)
                            .newCount(2)
                            .duplicateCount(0)
                            .failureCount(0)
                            .build();

            when(githubService.fetchValidReleases(eq("openai"), eq("openai-python"), anyInt()))
                    .thenReturn(releases);
            when(emergingTechInternalApi.createEmergingTechBatchInternal(anyString(), any()))
                    .thenReturn(ApiResponse.success(batchResponse));

            // When
            DataCollectionResultDto result = adapter.collectGitHubReleases("openai", "openai-python");

            // Then
            assertThat(result.newCount()).isEqualTo(2);
            assertThat(result.source()).isEqualTo("GITHUB_RELEASES");
            assertThat(result.failureCount()).isZero();
        }

        @Test
        @DisplayName("owner에서 provider 매핑 (anthropics → ANTHROPIC)")
        void collectGitHubReleases_provider매핑() {
            // Given
            when(githubService.fetchValidReleases(eq("anthropics"), any(), anyInt()))
                    .thenReturn(List.of(createRelease("v1.0", "Release", "notes")));

            InternalApiDto.EmergingTechBatchResponse batchResponse =
                    InternalApiDto.EmergingTechBatchResponse.builder()
                            .totalCount(1)
                            .newCount(1)
                            .duplicateCount(0)
                            .failureCount(0)
                            .build();

            when(emergingTechInternalApi.createEmergingTechBatchInternal(anyString(), any()))
                    .thenReturn(ApiResponse.success(batchResponse));

            // When
            DataCollectionResultDto result = adapter.collectGitHubReleases("anthropics", "sdk");

            // Then
            assertThat(result.provider()).isEqualTo("ANTHROPIC");
        }

        @Test
        @DisplayName("릴리즈 없을 시 빈 결과")
        void collectGitHubReleases_릴리즈없음() {
            // Given
            when(githubService.fetchValidReleases(any(), any(), anyInt()))
                    .thenReturn(List.of());

            // When
            DataCollectionResultDto result = adapter.collectGitHubReleases("owner", "repo");

            // Then
            assertThat(result.totalCollected()).isZero();
            assertThat(result.newCount()).isZero();
            assertThat(result.failureCount()).isZero();
        }

        @Test
        @DisplayName("Internal API 배치 호출 실패 시 failure 반환")
        void collectGitHubReleases_API실패() {
            // Given
            when(githubService.fetchValidReleases(any(), any(), anyInt()))
                    .thenReturn(List.of(createRelease("v1.0", "Release", "notes")));
            when(emergingTechInternalApi.createEmergingTechBatchInternal(anyString(), any()))
                    .thenThrow(new RuntimeException("API 오류"));

            // When
            DataCollectionResultDto result = adapter.collectGitHubReleases("owner", "repo");

            // Then
            assertThat(result.failureMessages()).isNotEmpty();
            assertThat(result.summary()).contains("실패");
        }

        @Test
        @DisplayName("API 응답 null 시 failure 반환")
        void collectGitHubReleases_응답null() {
            // Given
            when(githubService.fetchValidReleases(any(), any(), anyInt()))
                    .thenReturn(List.of(createRelease("v1.0", "Release", "notes")));
            when(emergingTechInternalApi.createEmergingTechBatchInternal(anyString(), any()))
                    .thenReturn(null);

            // When
            DataCollectionResultDto result = adapter.collectGitHubReleases("owner", "repo");

            // Then
            assertThat(result.failureMessages()).isNotEmpty();
            assertThat(result.summary()).contains("실패");
        }
    }

    // ========== collectRssFeeds 테스트 ==========

    @Nested
    @DisplayName("collectRssFeeds")
    class CollectRssFeeds {

        @Test
        @DisplayName("정상 수집 및 저장")
        void collectRssFeeds_정상수집() {
            // Given
            List<RssFeedItem> items = List.of(
                    createRssFeedItem("Title1", "https://example.com/1"),
                    createRssFeedItem("Title2", "https://example.com/2")
            );

            InternalApiDto.EmergingTechBatchResponse batchResponse =
                    InternalApiDto.EmergingTechBatchResponse.builder()
                            .totalCount(2)
                            .newCount(1)
                            .duplicateCount(1)
                            .failureCount(0)
                            .build();

            when(rssService.fetchRssFeeds(eq("OPENAI"))).thenReturn(items);
            when(emergingTechInternalApi.createEmergingTechBatchInternal(anyString(), any()))
                    .thenReturn(ApiResponse.success(batchResponse));

            // When
            DataCollectionResultDto result = adapter.collectRssFeeds("OPENAI");

            // Then
            assertThat(result.source()).isEqualTo("RSS_FEEDS");
            assertThat(result.newCount()).isEqualTo(1);
            assertThat(result.duplicateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("provider 필터 빈 값 - 전체 수집")
        void collectRssFeeds_전체수집() {
            // Given
            when(rssService.fetchRssFeeds(eq(""))).thenReturn(List.of());

            // When
            DataCollectionResultDto result = adapter.collectRssFeeds("");

            // Then
            assertThat(result.totalCollected()).isZero();
        }

        @Test
        @DisplayName("피드 없을 시 빈 결과")
        void collectRssFeeds_피드없음() {
            // Given
            when(rssService.fetchRssFeeds(any())).thenReturn(List.of());

            // When
            DataCollectionResultDto result = adapter.collectRssFeeds("GOOGLE");

            // Then
            assertThat(result.totalCollected()).isZero();
        }
    }

    // ========== collectScrapedArticles 테스트 ==========

    @Nested
    @DisplayName("collectScrapedArticles")
    class CollectScrapedArticles {

        @Test
        @DisplayName("정상 수집 및 저장")
        void collectScrapedArticles_정상수집() {
            // Given
            List<ScrapedTechArticle> articles = List.of(
                    createScrapedArticle("Article1", "https://anthropic.com/blog/1"),
                    createScrapedArticle("Article2", "https://anthropic.com/blog/2")
            );

            InternalApiDto.EmergingTechBatchResponse batchResponse =
                    InternalApiDto.EmergingTechBatchResponse.builder()
                            .totalCount(2)
                            .newCount(2)
                            .duplicateCount(0)
                            .failureCount(0)
                            .build();

            when(scraperService.scrapeArticles(eq("ANTHROPIC"))).thenReturn(articles);
            when(emergingTechInternalApi.createEmergingTechBatchInternal(anyString(), any()))
                    .thenReturn(ApiResponse.success(batchResponse));

            // When
            DataCollectionResultDto result = adapter.collectScrapedArticles("ANTHROPIC");

            // Then
            assertThat(result.source()).isEqualTo("WEB_SCRAPING");
            assertThat(result.newCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("provider 필터 적용 (META)")
        void collectScrapedArticles_META필터() {
            // Given
            when(scraperService.scrapeArticles(eq("META"))).thenReturn(List.of());

            // When
            DataCollectionResultDto result = adapter.collectScrapedArticles("META");

            // Then
            assertThat(result.provider()).isEqualTo("META");
        }

        @Test
        @DisplayName("아티클 없을 시 빈 결과")
        void collectScrapedArticles_아티클없음() {
            // Given
            when(scraperService.scrapeArticles(any())).thenReturn(List.of());

            // When
            DataCollectionResultDto result = adapter.collectScrapedArticles("");

            // Then
            assertThat(result.totalCollected()).isZero();
        }

        @Test
        @DisplayName("Internal API 오류 시 failure 반환")
        void collectScrapedArticles_API오류() {
            // Given
            when(scraperService.scrapeArticles(any()))
                    .thenReturn(List.of(createScrapedArticle("Title", "url")));
            when(emergingTechInternalApi.createEmergingTechBatchInternal(anyString(), any()))
                    .thenThrow(new RuntimeException("서버 오류"));

            // When
            DataCollectionResultDto result = adapter.collectScrapedArticles("ANTHROPIC");

            // Then
            assertThat(result.failureMessages()).isNotEmpty();
        }
    }

    // ========== 헬퍼 메서드 ==========

    private GitHubDto.Release createRelease(String tagName, String name, String body) {
        return GitHubDto.Release.builder()
                .id(1L)
                .tagName(tagName)
                .name(name)
                .body(body)
                .htmlUrl("https://github.com/owner/repo/releases/tag/" + tagName)
                .publishedAt("2024-01-15T10:00:00Z")
                .draft(false)
                .prerelease(false)
                .build();
    }

    private RssFeedItem createRssFeedItem(String title, String link) {
        // link에 openai.com 포함해야 resolveRssProvider에서 OPENAI 반환
        String validLink = link.contains("openai.com") || link.contains("blog.google")
                ? link : "https://openai.com/blog/" + link.hashCode();
        return RssFeedItem.builder()
                .title(title)
                .link(validLink)
                .description("Description")
                .publishedDate(LocalDateTime.now())
                .build();
    }

    private ScrapedTechArticle createScrapedArticle(String title, String url) {
        return ScrapedTechArticle.builder()
                .title(title)
                .url(url)
                .summary("Summary")
                .providerName("ANTHROPIC")
                .publishedDate(LocalDateTime.now())
                .build();
    }
}
