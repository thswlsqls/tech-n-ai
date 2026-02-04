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
            if (provider == null) {
                log.warn("RSS item 스킵: 알 수 없는 소스 URL - link={}", item.link());
                return null;
            }

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
        return null;
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
