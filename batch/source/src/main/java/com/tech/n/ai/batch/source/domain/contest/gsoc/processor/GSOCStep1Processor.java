package com.tech.n.ai.batch.source.domain.contest.gsoc.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * GSOC Step1 Processor
 * ScrapedContestItem → ContestCreateRequest 변환
 * 
 * Google Summer of Code 공식 문서 참고:
 * https://summerofcode.withgoogle.com/
 * 
 * ScrapedContestItem 객체 필드:
 * - title: String (제목)
 * - url: String (URL)
 * - description: String (설명)
 * - startDate: LocalDateTime (시작일시)
 * - endDate: LocalDateTime (종료일시)
 * - organizer: String (주최자)
 * - location: String (장소)
 * - category: String (카테고리/태그)
 * - prize: String (상금/보상)
 * - imageUrl: String (이미지 URL)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class GSOCStep1Processor implements ItemProcessor<ScrapedContestItem, ContestCreateRequest> {

    /**
     * Google Summer of Code 출처의 sourceId
     * TODO: SourcesDocument에서 Google Summer of Code 출처의 ID를 조회하도록 구현 필요
     */
    private static final String GSOC_SOURCE_ID = "507f1f77bcf86cd799439026";

    @Override
    public @Nullable ContestCreateRequest process(ScrapedContestItem item) throws Exception {
        if (item == null) {
            log.warn("GSOC scraped contest item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("GSOC scraped contest item title is null or blank, skipping item: {}", item.url());
            return null;
        }

        // 날짜/시간 검증 (GSOC는 날짜가 없을 수 있음)
        if (item.startDate() == null) {
            log.warn("GSOC scraped contest item startDate is null, skipping item: {}", item.title());
            return null;
        }

        if (item.endDate() == null) {
            log.warn("GSOC scraped contest item endDate is null, skipping item: {}", item.title());
            return null;
        }

        // URL 검증
        String url = item.url();
        if (url == null || url.isBlank()) {
            log.warn("GSOC scraped contest item url is null or blank, skipping item: {}", item.title());
            return null;
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
            .sourceName("Google Summer of Code")
            .prize(item.prize())
            .participants(null) // GSOC에서 제공하지 않음
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(GSOC_SOURCE_ID)
            .title(item.title())
            .startDate(item.startDate())
            .endDate(item.endDate())
            .description(description)
            .url(url)
            .metadata(metadata)
            .build();
    }

    /**
     * ScrapedContestItem 객체에서 태그 추출
     */
    private List<String> extractTags(ScrapedContestItem item) {
        List<String> tags = new ArrayList<>();
        
        if (item.category() != null && !item.category().isBlank()) {
            tags.add(item.category());
        }
        
        // location이 있으면 태그로 추가
        if (item.location() != null && !item.location().isBlank()) {
            tags.add(item.location());
        }
        
        // organizer가 있으면 태그로 추가
        if (item.organizer() != null && !item.organizer().isBlank()) {
            tags.add(item.organizer());
        }
        
        return tags;
    }
}
