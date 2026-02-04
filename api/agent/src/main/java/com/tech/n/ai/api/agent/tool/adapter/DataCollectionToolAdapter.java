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
