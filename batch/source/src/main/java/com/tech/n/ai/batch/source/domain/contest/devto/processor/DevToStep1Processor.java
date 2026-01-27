package com.tech.n.ai.batch.source.domain.contest.devto.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.Article;
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
 * DevTo Step1 Processor
 * DevToDto.Article → ContestCreateRequest 변환
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
 * 
 * Note: Dev.to는 주로 뉴스/아티클을 제공하지만, Contest 정보로 변환합니다.
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class DevToStep1Processor implements ItemProcessor<Article, ContestCreateRequest> {

    private static final String SOURCE_URL = "https://dev.to";
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
        
        log.info("Dev.to (contest) source initialized from Redis: sourceId={}", sourceId);
    }

    @Override
    public @Nullable ContestCreateRequest process(Article item) throws Exception {
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
        LocalDateTime startDate = parseDateTime(item.publishedAt());
        if (startDate == null) {
            startDate = parseDateTime(item.publishedTimestamp());
        }
        if (startDate == null) {
            startDate = parseDateTime(item.createdAt());
        }
        
        // endDate는 startDate + 1일로 설정 (Dev.to articles는 종료 시간이 없으므로)
        LocalDateTime endDate = startDate != null ? startDate.plusDays(1) : null;

        // URL 생성
        String url = item.url();
        if (url == null || url.isBlank()) {
            if (item.canonicalUrl() != null && !item.canonicalUrl().isBlank()) {
                url = item.canonicalUrl();
            } else if (item.path() != null && !item.path().isBlank()) {
                url = "https://dev.to" + item.path();
            } else {
                url = "https://dev.to";
            }
        }

        // 설명 생성
        String description = item.description();
        if (description == null || description.isBlank()) {
            description = "";
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        ContestCreateRequest.ContestMetadataRequest metadata = ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName("Dev.to API")
            .participants(item.commentsCount())
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
