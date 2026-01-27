package com.tech.n.ai.batch.source.domain.news.hackernews.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemResponse;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.redis.core.RedisTemplate;

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

    private static final String SOURCE_URL = "https://news.ycombinator.com";
    private static final String SOURCE_CATEGORY = "최신 IT 테크 뉴스 정보";
    
    private final RedisTemplate<String, String> redisTemplate;
    private String sourceId;

    @PostConstruct
    public void init() {
        String redisKey = SOURCE_URL + ":" + SOURCE_CATEGORY;
        this.sourceId = redisTemplate.opsForValue().get(redisKey);
        
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalStateException(
                String.format("Source ID not found in Redis cache: key=%s", redisKey));
        }
        
        log.info("Hacker News (news) source initialized from Redis: sourceId={}", sourceId);
    }

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
        String content = trimOrEmpty(item.text());

        // 요약 생성 (text 사용, 최대 길이 제한)
        String summary = trimOrEmpty(item.text());
        if (!summary.isEmpty() && summary.length() > 500) {
            summary = summary.substring(0, 500) + "...";
        }

        // 작성자 추출
        String author = trimOrEmpty(item.by());

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
            .sourceId(sourceId)
            .title(trimOrEmpty(item.title()))
            .content(content)
            .summary(summary)
            .publishedAt(publishedAt)
            .url(trimOrEmpty(url))
            .author(author)
            .metadata(metadata)
            .build();
    }

    private String trimOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    /**
     * ItemResponse 객체에서 태그 추출
     */
    private List<String> extractTags(ItemResponse item) {
        List<String> tags = new ArrayList<>();
        
        if (item.type() != null && !item.type().isBlank()) {
            tags.add("type:" + item.type().trim());
        }
        
        if (item.by() != null && !item.by().isBlank()) {
            tags.add("author:" + item.by().trim());
        }
        
        return tags;
    }
}
