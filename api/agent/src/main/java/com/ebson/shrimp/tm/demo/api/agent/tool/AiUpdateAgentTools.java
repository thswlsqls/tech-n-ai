package com.tech.n.ai.api.agent.tool;

import com.tech.n.ai.api.agent.tool.adapter.AiUpdateToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.GitHubToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.ScraperToolAdapter;
import com.tech.n.ai.api.agent.tool.adapter.SlackToolAdapter;
import com.tech.n.ai.api.agent.tool.dto.AiUpdateDto;
import com.tech.n.ai.api.agent.tool.dto.GitHubReleaseDto;
import com.tech.n.ai.api.agent.tool.dto.ScrapedContentDto;
import com.tech.n.ai.api.agent.tool.dto.ToolResult;
import com.tech.n.ai.api.agent.tool.validation.ToolInputValidator;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI Update Agent용 LangChain4j Tool 모음
 * AI 업데이트 자동화 파이프라인의 기능들을 Tool로 노출
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
public class AiUpdateAgentTools {

    private final GitHubToolAdapter githubAdapter;
    private final ScraperToolAdapter scraperAdapter;
    private final SlackToolAdapter slackAdapter;
    private final AiUpdateToolAdapter aiUpdateAdapter;

    private final AtomicInteger toolCallCount = new AtomicInteger(0);
    private final AtomicInteger postsCreatedCount = new AtomicInteger(0);
    private final AtomicInteger validationErrorCount = new AtomicInteger(0);

    public void resetCounters() {
        toolCallCount.set(0);
        postsCreatedCount.set(0);
        validationErrorCount.set(0);
    }

    public int getToolCallCount() {
        return toolCallCount.get();
    }

    public int getPostsCreatedCount() {
        return postsCreatedCount.get();
    }

    public int getValidationErrorCount() {
        return validationErrorCount.get();
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
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: fetch_github_releases(owner={}, repo={})", owner, repo);

        // 입력값 검증
        String validationError = ToolInputValidator.validateGitHubRepo(owner, repo);
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
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
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: scrape_web_page(url={})", url);

        // 입력값 검증
        String validationError = ToolInputValidator.validateUrl(url);
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
            log.warn("Tool 입력값 검증 실패: {}", validationError);
            return new ScrapedContentDto(null, validationError, url);
        }

        return scraperAdapter.scrape(url);
    }

    /**
     * 저장된 AI 업데이트 검색
     */
    @Tool(name = "search_ai_updates",
          value = "저장된 AI 업데이트를 검색합니다. 중복 확인이나 기존 데이터 조회에 사용합니다.")
    public List<AiUpdateDto> searchAiUpdates(
            @P("검색 키워드") String query,
            @P("AI 제공자 필터 (OPENAI, ANTHROPIC, GOOGLE, META 또는 빈 문자열)") String provider
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: search_ai_updates(query={}, provider={})", query, provider);

        // 입력값 검증
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

        return aiUpdateAdapter.search(query, provider);
    }

    /**
     * AI 업데이트 초안 포스트 생성
     */
    @Tool(name = "create_draft_post",
          value = "AI 업데이트 초안 포스트를 생성합니다. DRAFT 상태로 저장됩니다.")
    public ToolResult createDraftPost(
            @P("포스트 제목") String title,
            @P("포스트 요약 내용") String summary,
            @P("AI 제공자 (OPENAI, ANTHROPIC, GOOGLE, META)") String provider,
            @P("업데이트 유형 (MODEL_RELEASE, API_UPDATE, SDK_RELEASE, BLOG_POST)") String updateType,
            @P("원본 URL") String url
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: create_draft_post(title={}, provider={}, updateType={})",
                title, provider, updateType);

        // 입력값 검증
        String titleError = ToolInputValidator.validateRequired(title, "title");
        if (titleError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(titleError);
        }

        String summaryError = ToolInputValidator.validateRequired(summary, "summary");
        if (summaryError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(summaryError);
        }

        String providerError = ToolInputValidator.validateProviderRequired(provider);
        if (providerError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(providerError);
        }

        String updateTypeError = ToolInputValidator.validateUpdateType(updateType);
        if (updateTypeError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(updateTypeError);
        }

        String urlError = ToolInputValidator.validateUrl(url);
        if (urlError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(urlError);
        }

        ToolResult result = aiUpdateAdapter.createDraft(title, summary, provider, updateType, url);
        if (result.success()) {
            postsCreatedCount.incrementAndGet();
        }
        return result;
    }

    /**
     * 초안 포스트를 게시 상태로 변경
     */
    @Tool(name = "publish_post",
          value = "초안 포스트를 게시 상태로 변경합니다.")
    public ToolResult publishPost(
            @P("게시할 포스트 ID") String postId
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: publish_post(postId={})", postId);

        // 입력값 검증
        String validationError = ToolInputValidator.validateRequired(postId, "postId");
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(validationError);
        }

        return aiUpdateAdapter.publish(postId);
    }

    /**
     * Slack 채널에 메시지 전송
     */
    @Tool(name = "send_slack_notification",
          value = "Slack 채널에 메시지를 전송합니다. 관리자 알림에 사용합니다.")
    public ToolResult sendSlackNotification(
            @P("메시지 내용") String message
    ) {
        toolCallCount.incrementAndGet();
        log.info("Tool 호출: send_slack_notification(message={})", message);

        // 입력값 검증
        String validationError = ToolInputValidator.validateRequired(message, "message");
        if (validationError != null) {
            validationErrorCount.incrementAndGet();
            return ToolResult.failure(validationError);
        }

        return slackAdapter.sendNotification(message);
    }
}
