package com.tech.n.ai.batch.source.domain.news.hackernews.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemResponse;
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
 * HackerNews Step1 Processor (News)
 * HackerNewsDto.ItemResponse → NewsCreateRequest 변환
 * 
 * Hacker News API 공식 문서 참고:
 * https://github.com/HackerNews/API
 * 
 * ItemResponse 객체 필드:
 * - id: Long (아이템 ID)
 * - type: String (아이템 타입: "story", "comment", "job" 등)
 * - by: String (작성자)
 * - time: Long (생성 시간, Unix timestamp)
 * - title: String (제목)
 * - url: String (URL)
 * - text: String (본문)
 * - score: Integer (점수)
 * - descendants: Integer (댓글 수)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class NewsHackerNewsStep1Processor implements ItemProcessor<ItemResponse, NewsCreateRequest> {

    /**
     * Hacker News 출처의 sourceId (News용)
     * TODO: SourcesDocument에서 Hacker News 출처의 ID를 조회하도록 구현 필요
     */
    private static final String HACKERNEWS_SOURCE_ID = "507f1f77bcf86cd799439016";

    @Override
    public @Nullable NewsCreateRequest process(ItemResponse item) throws Exception {
        if (item == null) {
            log.warn("HackerNews item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("HackerNews item title is null or blank, skipping item: {}", item.id());
            return null;
        }

        // story 타입만 처리
        if (!"story".equals(item.type())) {
            log.debug("Skipping non-story item: type={}, id={}", item.type(), item.id());
            return null;
        }

        // 날짜/시간 변환 (Unix timestamp → LocalDateTime)
        LocalDateTime publishedAt = null;
        if (item.time() != null) {
            publishedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.time()), ZoneOffset.UTC);
        }
        if (publishedAt == null) {
            log.warn("HackerNews item publishedAt is null or invalid, skipping item: {}", item.id());
            return null;
        }

        // URL 생성
        String url = item.url();
        if (url == null || url.isBlank()) {
            if (item.id() != null) {
                url = "https://news.ycombinator.com/item?id=" + item.id();
            } else {
                log.warn("HackerNews item url is null or blank, skipping item: {}", item.id());
                return null;
            }
        }

        // 내용 생성 (text 사용)
        String content = item.text();
        if (content == null || content.isBlank()) {
            content = "";
        }

        // 요약 생성 (text 사용, 최대 길이 제한)
        String summary = item.text();
        if (summary == null || summary.isBlank()) {
            summary = "";
        } else if (summary.length() > 500) {
            summary = summary.substring(0, 500) + "...";
        }

        // 작성자 추출
        String author = item.by();
        if (author == null || author.isBlank()) {
            author = "";
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        NewsCreateRequest.NewsMetadataRequest metadata = NewsCreateRequest.NewsMetadataRequest.builder()
            .sourceName("Hacker News API")
            .tags(tags)
            .viewCount(item.descendants())
            .likeCount(item.score())
            .build();

        return NewsCreateRequest.builder()
            .sourceId(HACKERNEWS_SOURCE_ID)
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
     * ItemResponse 객체에서 태그 추출
     */
    private List<String> extractTags(ItemResponse item) {
        List<String> tags = new ArrayList<>();
        
        if (item.type() != null && !item.type().isBlank()) {
            tags.add("type:" + item.type());
        }
        
        if (item.by() != null && !item.by().isBlank()) {
            tags.add("author:" + item.by());
        }
        
        return tags;
    }
}
