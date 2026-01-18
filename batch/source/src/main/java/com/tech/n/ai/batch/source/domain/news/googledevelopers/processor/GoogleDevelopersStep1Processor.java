package com.tech.n.ai.batch.source.domain.news.googledevelopers.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Google Developers Step1 Processor
 * RssFeedItem → NewsCreateRequest 변환
 * 
 * Google Developers Blog RSS 피드 공식 문서 참고:
 * https://developers.googleblog.com/feeds/posts/default
 * 
 * RssFeedItem 객체 필드:
 * - title: String (제목)
 * - link: String (URL)
 * - description: String (설명/내용)
 * - publishedDate: LocalDateTime (발행일시)
 * - author: String (작성자)
 * - category: String (카테고리/태그)
 * - guid: String (고유 식별자)
 * - imageUrl: String (이미지 URL)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class GoogleDevelopersStep1Processor implements ItemProcessor<RssFeedItem, NewsCreateRequest> {

    /**
     * Google Developers Blog 출처의 sourceId
     * TODO: SourcesDocument에서 Google Developers Blog 출처의 ID를 조회하도록 구현 필요
     */
    private static final String GOOGLE_DEVELOPERS_SOURCE_ID = "507f1f77bcf86cd799439022";

    @Override
    public @Nullable NewsCreateRequest process(RssFeedItem item) throws Exception {
        if (item == null) {
            log.warn("Google Developers RSS feed item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("Google Developers RSS feed item title is null or blank, skipping item: {}", item.guid());
            return null;
        }

        // 날짜/시간 검증
        if (item.publishedDate() == null) {
            log.warn("Google Developers RSS feed item publishedDate is null, skipping item: {}", item.title());
            return null;
        }

        // URL 검증
        String url = item.link();
        if (url == null || url.isBlank()) {
            log.warn("Google Developers RSS feed item url is null or blank, skipping item: {}", item.title());
            return null;
        }

        // 내용 생성 (description 사용)
        String content = item.description();
        if (content == null || content.isBlank()) {
            content = "";
        }

        // 요약 생성 (description 사용, 최대 길이 제한)
        String summary = item.description();
        if (summary == null || summary.isBlank()) {
            summary = "";
        } else if (summary.length() > 500) {
            summary = summary.substring(0, 500) + "...";
        }

        // 작성자 추출
        String author = item.author();
        if (author == null || author.isBlank()) {
            author = "";
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        NewsCreateRequest.NewsMetadataRequest metadata = NewsCreateRequest.NewsMetadataRequest.builder()
            .sourceName("Google Developers Blog RSS")
            .tags(tags)
            .build();

        return NewsCreateRequest.builder()
            .sourceId(GOOGLE_DEVELOPERS_SOURCE_ID)
            .title(item.title())
            .content(content)
            .summary(summary)
            .publishedAt(item.publishedDate())
            .url(url)
            .author(author)
            .metadata(metadata)
            .build();
    }

    /**
     * RssFeedItem 객체에서 태그 추출
     */
    private List<String> extractTags(RssFeedItem item) {
        List<String> tags = new ArrayList<>();
        
        if (item.category() != null && !item.category().isBlank()) {
            tags.add(item.category());
        }
        
        return tags;
    }
}
