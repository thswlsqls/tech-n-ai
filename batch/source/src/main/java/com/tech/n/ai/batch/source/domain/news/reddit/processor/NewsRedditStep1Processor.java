package com.tech.n.ai.batch.source.domain.news.reddit.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.Post;
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
 * Reddit Step1 Processor (News)
 * RedditDto.Post → NewsCreateRequest 변환
 * 
 * Reddit API 공식 문서 참고:
 * https://www.reddit.com/dev/api
 * 
 * Post 객체 필드:
 * - id: String (포스트 ID)
 * - name: String (포스트 이름)
 * - title: String (제목)
 * - selftext: String (본문)
 * - author: String (작성자)
 * - created_utc: Long (생성 시간, Unix timestamp)
 * - score: Integer (점수)
 * - url: String (URL)
 * - permalink: String (퍼머링크)
 * - subreddit: String (서브레딧)
 * - numComments: Integer (댓글 수)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class NewsRedditStep1Processor implements ItemProcessor<Post, NewsCreateRequest> {

    private static final String SOURCE_URL = "https://www.reddit.com";
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
        
        log.info("Reddit (news) source initialized from Redis: sourceId={}", sourceId);
    }

    @Override
    public @Nullable NewsCreateRequest process(Post item) throws Exception {
        if (item == null) {
            log.warn("Reddit post item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("Reddit post title is null or blank, skipping item: {}", item.id());
            return null;
        }

        // 날짜/시간 변환 (Unix timestamp → LocalDateTime)
        LocalDateTime publishedAt = null;
        if (item.createdUtc() != null) {
            publishedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.createdUtc()), ZoneOffset.UTC);
        }
        if (publishedAt == null) {
            log.warn("Reddit post publishedAt is null or invalid, skipping item: {}", item.id());
            return null;
        }

        // URL 생성
        String url = item.url();
        if (url == null || url.isBlank()) {
            String permalink = item.permalink();
            if (permalink != null && !permalink.isBlank()) {
                url = "https://www.reddit.com" + permalink;
            } else {
                log.warn("Reddit post url is null or blank, skipping item: {}", item.id());
                return null;
            }
        }

        // 내용 생성 (selftext 사용)
        String content = trimOrEmpty(item.selftext());

        // 요약 생성 (selftext 사용, 최대 길이 제한)
        String summary = trimOrEmpty(item.selftext());
        if (!summary.isEmpty() && summary.length() > 500) {
            summary = summary.substring(0, 500) + "...";
        }

        // 작성자 추출
        String author = trimOrEmpty(item.author());

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        NewsCreateRequest.NewsMetadataRequest metadata = NewsCreateRequest.NewsMetadataRequest.builder()
            .sourceName("Reddit API")
            .tags(tags)
            .viewCount(item.numComments())
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
     * Post 객체에서 태그 추출
     */
    private List<String> extractTags(Post item) {
        List<String> tags = new ArrayList<>();
        
        if (item.subreddit() != null && !item.subreddit().isBlank()) {
            tags.add("subreddit:" + item.subreddit().trim());
        }
        
        if (item.domain() != null && !item.domain().isBlank()) {
            tags.add("domain:" + item.domain().trim());
        }
        
        return tags;
    }
}
