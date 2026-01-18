package com.tech.n.ai.batch.source.domain.news.newsapi.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto.Article;
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
 * NewsAPI Step1 Processor
 * NewsAPIDto.Article → NewsCreateRequest 변환
 * 
 * NewsAPI 공식 문서 참고:
 * https://newsapi.org/docs
 * 
 * Article 객체 필드:
 * - source: Source (출처 정보)
 * - author: String (작성자)
 * - title: String (제목)
 * - description: String (설명)
 * - url: String (URL)
 * - urlToImage: String (이미지 URL)
 * - publishedAt: String (발행 시간, ISO 8601 형식)
 * - content: String (본문 내용)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class NewsApiStep1Processor implements ItemProcessor<Article, NewsCreateRequest> {

    /**
     * NewsAPI 출처의 sourceId
     * TODO: SourcesDocument에서 NewsAPI 출처의 ID를 조회하도록 구현 필요
     */
    private static final String NEWSAPI_SOURCE_ID = "507f1f77bcf86cd799439020";

    @Override
    public @Nullable NewsCreateRequest process(Article item) throws Exception {
        if (item == null) {
            log.warn("NewsAPI article item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("NewsAPI article title is null or blank, skipping item: {}", item.url());
            return null;
        }

        // 날짜/시간 변환 (ISO 8601 → LocalDateTime)
        LocalDateTime publishedAt = parseDateTime(item.publishedAt());
        if (publishedAt == null) {
            log.warn("NewsAPI article publishedAt is null or invalid, skipping item: {}", item.url());
            return null;
        }

        // URL 검증
        String url = item.url();
        if (url == null || url.isBlank()) {
            log.warn("NewsAPI article url is null or blank, skipping item: {}", item.title());
            return null;
        }

        // 내용 생성 (description 또는 content 사용)
        String content = item.content();
        if (content == null || content.isBlank()) {
            content = item.description();
        }
        if (content == null || content.isBlank()) {
            content = "";
        }

        // 요약 생성 (description 사용)
        String summary = item.description();
        if (summary == null || summary.isBlank()) {
            summary = "";
        }

        // 작성자 추출
        String author = item.author();
        if (author == null || author.isBlank()) {
            if (item.source() != null && item.source().name() != null) {
                author = item.source().name();
            } else {
                author = "";
            }
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        NewsCreateRequest.NewsMetadataRequest metadata = NewsCreateRequest.NewsMetadataRequest.builder()
            .sourceName(item.source() != null && item.source().name() != null 
                ? item.source().name() 
                : "NewsAPI")
            .tags(tags)
            .build();

        return NewsCreateRequest.builder()
            .sourceId(NEWSAPI_SOURCE_ID)
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
            // NewsAPI는 ISO 8601 형식 사용 (예: "2024-01-15T10:30:00Z")
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
        
        if (item.source() != null) {
            if (item.source().id() != null && !item.source().id().isBlank()) {
                tags.add("source:" + item.source().id());
            }
            if (item.source().name() != null && !item.source().name().isBlank()) {
                tags.add("sourceName:" + item.source().name());
            }
        }
        
        return tags;
    }
}
