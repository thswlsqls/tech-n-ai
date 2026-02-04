package com.tech.n.ai.api.agent.tool;

import com.tech.n.ai.api.agent.metrics.ToolExecutionMetrics;
import com.tech.n.ai.api.agent.tool.adapter.AnalyticsToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.DataCollectionToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.EmergingTechToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.GitHubToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.ScraperToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.SlackToolAdapter;
import com.tech.n.ai.api.agent.tool.dto.DataCollectionResultDto;
import com.tech.n.ai.api.agent.tool.dto.EmergingTechDto;
import com.tech.n.ai.api.agent.tool.dto.GitHubReleaseDto;
import com.tech.n.ai.api.agent.tool.dto.ScrapedContentDto;
import com.tech.n.ai.api.agent.tool.dto.StatisticsDto;
import com.tech.n.ai.api.agent.tool.dto.ToolResult;
import com.tech.n.ai.api.agent.tool.dto.WordFrequencyDto;
import com.tech.n.ai.api.agent.tool.validation.ToolInputValidator;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Emerging Tech Agent용 LangChain4j Tool 모음
 *
 * <p>모든 Tool 메서드는 다음 패턴을 따름:
 * <ol>
 *   <li>입력값 검증 (ToolInputValidator 사용)</li>
 *   <li>검증 실패 시 빈 결과 또는 ToolResult.failure 반환</li>
 *   <li>검증 성공 시 Adapter를 통해 외부 API 호출</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergingTechAgentTools {

    private final GitHubToolAdapter githubAdapter;
    private final ScraperToolAdapter scraperAdapter;
    private final SlackToolAdapter slackAdapter;
    private final EmergingTechToolAdapter emergingTechAdapter;
    private final AnalyticsToolAdapter analyticsAdapter;
    private final DataCollectionToolAdapter dataCollectionAdapter;

    /** 실행별 메트릭 (동시 실행 격리를 위해 ThreadLocal 사용) */
    private final ThreadLocal<ToolExecutionMetrics> currentMetrics = new ThreadLocal<>();

    /**
     * 실행 전 메트릭 바인딩
     * @param metrics 이번 실행에서 사용할 메트릭 인스턴스
     */
    public void bindMetrics(ToolExecutionMetrics metrics) {
        currentMetrics.set(metrics);
    }

    /**
     * 실행 후 메트릭 해제 (메모리 누수 방지)
     */
    public void unbindMetrics() {
        currentMetrics.remove();
    }

    private ToolExecutionMetrics metrics() {
        ToolExecutionMetrics m = currentMetrics.get();
        if (m == null) {
            throw new IllegalStateException("ToolExecutionMetrics가 바인딩되지 않았습니다. bindMetrics()를 먼저 호출하세요.");
        }
        return m;
    }

    /**
     * GitHub 저장소의 최신 릴리스 목록 조회
     */
    @Tool(name = "fetch_github_releases",
          value = "GitHub 저장소의 최신 릴리스 목록을 가져옵니다. SDK 업데이트 확인에 사용합니다.")
    public List<GitHubReleaseDto> fetchGitHubReleases(
            @P("저장소 소유자 (예: openai, anthropics)") String owner,
            @P("저장소 이름 (예: openai-python, anthropic-sdk-python)") String repo
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: fetch_github_releases(owner={}, repo={})", owner, repo);

        String validationError = ToolInputValidator.validateGitHubRepo(owner, repo);
        if (validationError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", validationError);
            return List.of();
        }

        return githubAdapter.getReleases(owner, repo);
    }

    /**
     * 웹 페이지 크롤링하여 텍스트 내용 추출
     */
    @Tool(name = "scrape_web_page",
          value = "웹 페이지를 크롤링하여 텍스트 내용을 추출합니다. 블로그 포스트 내용 확인에 사용합니다.")
    public ScrapedContentDto scrapeWebPage(
            @P("크롤링할 웹 페이지 URL") String url
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: scrape_web_page(url={})", url);

        String validationError = ToolInputValidator.validateUrl(url);
        if (validationError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", validationError);
            return new ScrapedContentDto(null, validationError, url);
        }

        return scraperAdapter.scrape(url);
    }

    /**
     * 저장된 Emerging Tech 업데이트 검색
     */
    @Tool(name = "search_emerging_techs",
          value = "저장된 Emerging Tech 업데이트를 검색합니다. 중복 확인이나 기존 데이터 조회에 사용합니다.")
    public List<EmergingTechDto> searchEmergingTechs(
            @P("검색 키워드") String query,
            @P("기술 제공자 필터 (OPENAI, ANTHROPIC, GOOGLE, META, XAI 또는 빈 문자열)") String provider
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: search_emerging_techs(query={}, provider={})", query, provider);

        String queryError = ToolInputValidator.validateRequired(query, "query");
        if (queryError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", queryError);
            return List.of();
        }

        String providerError = ToolInputValidator.validateProviderOptional(provider);
        if (providerError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", providerError);
            return List.of();
        }

        return emergingTechAdapter.search(query, provider);
    }

    /**
     * Provider/SourceType/UpdateType별 통계 집계
     */
    @Tool(name = "get_emerging_tech_statistics",
          value = "조회 기간 기준으로 EmergingTech 데이터를 Provider, SourceType, UpdateType별로 집계합니다. "
                + "결과를 Markdown 표와 Mermaid 차트로 정리하여 보여줄 수 있습니다.")
    public StatisticsDto getStatistics(
        @P("집계 기준 필드: provider, source_type, update_type") String groupBy,
        @P("조회 시작일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String startDate,
        @P("조회 종료일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String endDate
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: get_emerging_tech_statistics(groupBy={}, startDate={}, endDate={})",
                groupBy, startDate, endDate);

        // 입력 검증
        String groupByError = ToolInputValidator.validateGroupByField(groupBy);
        if (groupByError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", groupByError);
            return new StatisticsDto(groupBy, startDate, endDate, 0, List.of());
        }

        String startDateError = ToolInputValidator.validateDateOptional(startDate, "startDate");
        if (startDateError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", startDateError);
            return new StatisticsDto(groupBy, startDate, endDate, 0, List.of());
        }

        String endDateError = ToolInputValidator.validateDateOptional(endDate, "endDate");
        if (endDateError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", endDateError);
            return new StatisticsDto(groupBy, startDate, endDate, 0, List.of());
        }

        // groupBy를 MongoDB 필드명으로 정규화
        String resolvedGroupBy = ToolInputValidator.resolveGroupByField(groupBy);
        return analyticsAdapter.getStatistics(resolvedGroupBy, startDate, endDate);
    }

    /**
     * title/summary 텍스트 키워드 빈도 분석
     */
    @Tool(name = "analyze_text_frequency",
          value = "EmergingTech 도큐먼트의 title, summary에서 주요 키워드 빈도를 분석합니다. "
                + "Mermaid 차트나 Word Cloud 형태로 결과를 정리할 수 있습니다.")
    public WordFrequencyDto analyzeTextFrequency(
        @P("Provider 필터 (OPENAI, ANTHROPIC 등, 빈 문자열이면 전체)") String provider,
        @P("조회 시작일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String startDate,
        @P("조회 종료일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String endDate,
        @P("상위 키워드 개수 (기본값 20)") int topN
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: analyze_text_frequency(provider={}, startDate={}, endDate={}, topN={})",
                provider, startDate, endDate, topN);

        String providerError = ToolInputValidator.validateProviderOptional(provider);
        if (providerError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", providerError);
            return new WordFrequencyDto(0, "", List.of(), List.of());
        }

        String startDateError = ToolInputValidator.validateDateOptional(startDate, "startDate");
        if (startDateError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", startDateError);
            return new WordFrequencyDto(0, "", List.of(), List.of());
        }

        String endDateError = ToolInputValidator.validateDateOptional(endDate, "endDate");
        if (endDateError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", endDateError);
            return new WordFrequencyDto(0, "", List.of(), List.of());
        }

        int effectiveTopN = (topN > 0 && topN <= 100) ? topN : 20;
        return analyticsAdapter.analyzeTextFrequency(provider, startDate, endDate, effectiveTopN);
    }

    /**
     * Slack 채널에 메시지 전송
     */
    @Tool(name = "send_slack_notification",
          value = "Slack 채널에 메시지를 전송합니다. 관리자 알림에 사용합니다.")
    public ToolResult sendSlackNotification(
            @P("메시지 내용") String message
    ) {
        metrics().incrementToolCall();
        log.info("Tool 호출: send_slack_notification(message={})", message);

        String validationError = ToolInputValidator.validateRequired(message, "message");
        if (validationError != null) {
            metrics().incrementValidationError();
            return ToolResult.failure(validationError);
        }

        return slackAdapter.sendNotification(message);
    }

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

        String correctedOwner = ToolInputValidator.correctGitHubOwner(owner);
        if (!correctedOwner.equals(owner)) {
            log.info("Tool 호출: collect_github_releases(owner={} → {}, repo={})", owner, correctedOwner, repo);
        } else {
            log.info("Tool 호출: collect_github_releases(owner={}, repo={})", owner, repo);
        }

        String validationError = ToolInputValidator.validateGitHubRepo(correctedOwner, repo);
        if (validationError != null) {
            metrics().incrementValidationError();
            log.warn("Tool 입력값 검증 실패: {}", validationError);
            return DataCollectionResultDto.failure("GITHUB_RELEASES", "", 0, validationError);
        }

        return dataCollectionAdapter.collectGitHubReleases(correctedOwner, repo);
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
}
