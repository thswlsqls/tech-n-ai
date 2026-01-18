package com.tech.n.ai.batch.source.domain.news.devto.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.Article;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * DevTo Step1 Processor (News)
 * DevToDto.Article → NewsCreateRequest 변환
 * 
 * Dev.to API 공식 문서 참고:
 * https://docs.forem.com/api
 * 
 * Article 객체 필드:
 * - id: Integer (아티클 ID)
 * - title: String (제목)
 * - description: String (설명)
 * - url: String (URL)
 * - publishedAt: String (발행 시간, ISO 8601 형식)
 * - publishedTimestamp: String (발행 타임스탬프)
 * - createdAt: String (생성 시간, ISO 8601 형식)
 * - tagList: List<String> (태그 목록)
 * - commentsCount: Integer (댓글 수)
 * - positiveReactionsCount: Integer (좋아요 수)
 * - user: User (작성자 정보)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class NewsDevToStep1Processor implements ItemProcessor<Article, NewsCreateRequest> {

    /**
     * Dev.to 출처의 sourceId (News용)
     * TODO: SourcesDocument에서 Dev.to 출처의 ID를 조회하도록 구현 필요
     */
    private static final String DEVTO_SOURCE_ID = "507f1f77bcf86cd799439017";

    @Override
    public @Nullable NewsCreateRequest process(Article item) throws Exception {
        if (item == null) {
            log.warn("DevTo article item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("DevTo article title is null or blank, skipping item: {}", item.id());
            return null;
        }

        // 날짜/시간 변환 (ISO 8601 → LocalDateTime)
        LocalDateTime publishedAt = parseDateTime(item.publishedAt());
        if (publishedAt == null) {
            publishedAt = parseDateTime(item.publishedTimestamp());
        }
        if (publishedAt == null) {
            publishedAt = parseDateTime(item.createdAt());
        }
        if (publishedAt == null) {
            log.warn("DevTo article publishedAt is null or invalid, skipping item: {}", item.id());
            return null;
        }

        // URL 생성
        String url = item.url();
        if (url == null || url.isBlank()) {
            if (item.canonicalUrl() != null && !item.canonicalUrl().isBlank()) {
                url = item.canonicalUrl();
            } else if (item.path() != null && !item.path().isBlank()) {
                url = "https://dev.to" + item.path();
            } else {
                log.warn("DevTo article url is null or blank, skipping item: {}", item.id());
                return null;
            }
        }

        // 내용 생성 (description 사용)
        String content = item.description();
        if (content == null || content.isBlank()) {
            content = "";
        }

        // 요약 생성 (description 사용)
        String summary = item.description();
        if (summary == null || summary.isBlank()) {
            summary = "";
        }

        // 작성자 추출
        String author = "";
        if (item.user() != null) {
            if (item.user().name() != null && !item.user().name().isBlank()) {
                author = item.user().name();
            } else if (item.user().username() != null && !item.user().username().isBlank()) {
                author = item.user().username();
            }
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        NewsCreateRequest.NewsMetadataRequest metadata = NewsCreateRequest.NewsMetadataRequest.builder()
            .sourceName("Dev.to API")
            .tags(tags)
            .viewCount(item.commentsCount())
            .likeCount(item.positiveReactionsCount())
            .build();

        return NewsCreateRequest.builder()
            .sourceId(DEVTO_SOURCE_ID)
            .title(item.title())
            .content(content)
            .summary(summary)
            .publishedAt(publishedAt)
            .url(url)
            .author(author)
            .metadata(metadata)
            .build();
    }

    /**
     * 날짜/시간 문자열을 LocalDateTime으로 변환
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }

        try {
            Instant instant = Instant.parse(dateTimeStr);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse dateTime: {}, error: {}", dateTimeStr, e.getMessage());
            return null;
        }
    }

    /**
     * Article 객체에서 태그 추출
     */
    private List<String> extractTags(Article item) {
        List<String> tags = new ArrayList<>();
        
        if (item.tagList() != null) {
            tags.addAll(item.tagList());
        }
        
        if (item.flareTag() != null && item.flareTag().name() != null) {
            tags.add("flare:" + item.flareTag().name());
        }
        
        if (item.organization() != null && item.organization().name() != null) {
            tags.add("org:" + item.organization().name());
        }
        
        return tags;
    }
}
