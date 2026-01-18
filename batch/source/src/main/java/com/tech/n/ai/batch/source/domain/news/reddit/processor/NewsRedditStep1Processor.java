package com.tech.n.ai.batch.source.domain.news.reddit.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.Post;
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

    /**
     * Reddit 출처의 sourceId (News용)
     * TODO: SourcesDocument에서 Reddit 출처의 ID를 조회하도록 구현 필요
     */
    private static final String REDDIT_SOURCE_ID = "507f1f77bcf86cd799439015";

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
        String content = item.selftext();
        if (content == null || content.isBlank()) {
            content = "";
        }

        // 요약 생성 (selftext 사용, 최대 길이 제한)
        String summary = item.selftext();
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
            .sourceName("Reddit API")
            .tags(tags)
            .viewCount(item.numComments())
            .likeCount(item.score())
            .build();

        return NewsCreateRequest.builder()
            .sourceId(REDDIT_SOURCE_ID)
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
