package com.tech.n.ai.batch.source.domain.contest.devpost.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Devpost Step1 Processor
 * ScrapedContestItem → ContestCreateRequest 변환
 * 
 * Devpost 공식 문서 참고:
 * https://devpost.com/hackathons
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
public class DevpostStep1Processor implements ItemProcessor<ScrapedContestItem, ContestCreateRequest> {

    private static final String SOURCE_URL = "https://devpost.com";
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
        
        log.info("Devpost source initialized from Redis: sourceId={}", sourceId);
    }

    @Override
    public @Nullable ContestCreateRequest process(ScrapedContestItem item) throws Exception {
        if (item == null) {
            log.warn("Devpost scraped contest item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("Devpost scraped contest item title is null or blank, skipping item: {}", item.url());
            return null;
        }

        // 날짜/시간 검증
        if (item.startDate() == null) {
            log.warn("Devpost scraped contest item startDate is null, skipping item: {}", item.title());
            return null;
        }

        if (item.endDate() == null) {
            log.warn("Devpost scraped contest item endDate is null, skipping item: {}", item.title());
            return null;
        }

        // URL 검증
        String url = item.url();
        if (url == null || url.isBlank()) {
            log.warn("Devpost scraped contest item url is null or blank, skipping item: {}", item.title());
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
            .sourceName("Devpost")
            .prize(item.prize())
            .participants(null) // Devpost에서 제공하지 않음
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(sourceId)
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
