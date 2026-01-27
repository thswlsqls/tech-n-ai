package com.tech.n.ai.batch.source.domain.contest.reddit.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
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
 * Reddit Step1 Processor
 * RedditDto.Post → ContestCreateRequest 변환
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
 * 
 * Note: Reddit은 주로 뉴스/포스트를 제공하지만, Contest 정보로 변환합니다.
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class RedditStep1Processor implements ItemProcessor<Post, ContestCreateRequest> {

    private static final String SOURCE_URL = "https://www.reddit.com";
    private static final String SOURCE_CATEGORY = "개발자 대회 정보";
    
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
        
        log.info("Reddit (contest) source initialized from Redis: sourceId={}", sourceId);
    }

    @Override
    public @Nullable ContestCreateRequest process(Post item) throws Exception {
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
        LocalDateTime startDate = null;
        if (item.createdUtc() != null) {
            startDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.createdUtc()), ZoneOffset.UTC);
        }
        
        // endDate는 startDate + 1일로 설정 (Reddit posts는 종료 시간이 없으므로)
        LocalDateTime endDate = startDate != null ? startDate.plusDays(1) : null;

        // URL 생성
        String url = item.url();
        if (url == null || url.isBlank()) {
            String permalink = item.permalink();
            if (permalink != null && !permalink.isBlank()) {
                url = "https://www.reddit.com" + permalink;
            } else {
                url = "https://www.reddit.com";
            }
        }

        // 설명 생성
        String description = item.selftext();
        if (description == null || description.isBlank()) {
            description = "";
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        ContestCreateRequest.ContestMetadataRequest metadata = ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName("Reddit API")
            .participants(item.numComments())
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(sourceId)
            .title(item.title())
            .startDate(startDate)
            .endDate(endDate)
            .description(description)
            .url(url)
            .metadata(metadata)
            .build();
    }

    /**
     * Post 객체에서 태그 추출
     */
    private List<String> extractTags(Post item) {
        List<String> tags = new ArrayList<>();
        
        if (item.subreddit() != null && !item.subreddit().isBlank()) {
            tags.add("subreddit:" + item.subreddit());
        }
        
        if (item.domain() != null && !item.domain().isBlank()) {
            tags.add("domain:" + item.domain());
        }
        
        return tags;
    }
}
