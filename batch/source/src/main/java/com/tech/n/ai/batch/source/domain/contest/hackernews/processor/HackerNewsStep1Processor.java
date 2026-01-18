package com.tech.n.ai.batch.source.domain.contest.hackernews.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
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
 * HackerNews Step1 Processor
 * HackerNewsDto.ItemResponse → ContestCreateRequest 변환
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
 * 
 * Note: Hacker News는 주로 뉴스/스토리를 제공하지만, Contest 정보로 변환합니다.
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class HackerNewsStep1Processor implements ItemProcessor<ItemResponse, ContestCreateRequest> {

    /**
     * Hacker News 출처의 sourceId
     * TODO: SourcesDocument에서 Hacker News 출처의 ID를 조회하도록 구현 필요
     */
    private static final String HACKERNEWS_SOURCE_ID = "507f1f77bcf86cd799439016";

    @Override
    public @Nullable ContestCreateRequest process(ItemResponse item) throws Exception {
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
        LocalDateTime startDate = null;
        if (item.time() != null) {
            startDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.time()), ZoneOffset.UTC);
        }
        
        // endDate는 startDate + 1일로 설정 (Hacker News stories는 종료 시간이 없으므로)
        LocalDateTime endDate = startDate != null ? startDate.plusDays(1) : null;

        // URL 생성
        String url = item.url();
        if (url == null || url.isBlank()) {
            if (item.id() != null) {
                url = "https://news.ycombinator.com/item?id=" + item.id();
            } else {
                url = "https://news.ycombinator.com";
            }
        }

        // 설명 생성
        String description = item.text();
        if (description == null || description.isBlank()) {
            description = "";
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        ContestCreateRequest.ContestMetadataRequest metadata = ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName("Hacker News API")
            .participants(item.descendants())
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(HACKERNEWS_SOURCE_ID)
            .title(item.title())
            .startDate(startDate)
            .endDate(endDate)
            .description(description)
            .url(url)
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
