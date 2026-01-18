package com.tech.n.ai.batch.source.domain.contest.producthunt.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * ProductHunt Step1 Processor
 * ProductHunt GraphQL Response (Map) → ContestCreateRequest 변환
 * 
 * ProductHunt API 공식 문서 참고:
 * https://api.producthunt.com/v2/docs
 * 
 * ProductHunt는 GraphQL API를 사용하며, posts 데이터를 Contest로 변환합니다.
 * Post 객체 필드 (GraphQL 응답에서 추출):
 * - id: String (제품 ID)
 * - name: String (제품명)
 * - tagline: String (태그라인)
 * - description: String (설명)
 * - createdAt: String (생성 시간, ISO 8601 형식)
 * - featuredAt: String (피처된 시간, ISO 8601 형식)
 * - url: String (URL)
 * - website: String (웹사이트)
 * - topics: List (토픽 목록)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class ProductHuntStep1Processor implements ItemProcessor<Map<String, Object>, ContestCreateRequest> {

    /**
     * ProductHunt 출처의 sourceId
     * TODO: SourcesDocument에서 ProductHunt 출처의 ID를 조회하도록 구현 필요
     */
    private static final String PRODUCTHUNT_SOURCE_ID = "507f1f77bcf86cd799439014";

    @Override
    public @Nullable ContestCreateRequest process(Map<String, Object> item) throws Exception {
        if (item == null || item.isEmpty()) {
            log.warn("ProductHunt post item is null or empty");
            return null;
        }

        // 필수 필드 검증
        String name = (String) item.get("name");
        if (name == null || name.isBlank()) {
            log.warn("ProductHunt post name is null or blank, skipping item: {}", item.get("id"));
            return null;
        }

        // 날짜/시간 변환 (ISO 8601 → LocalDateTime)
        LocalDateTime startDate = parseDateTime((String) item.get("featuredAt"));
        if (startDate == null) {
            startDate = parseDateTime((String) item.get("createdAt"));
        }
        
        // endDate는 startDate + 1일로 설정 (ProductHunt posts는 종료 시간이 없으므로)
        LocalDateTime endDate = startDate != null ? startDate.plusDays(1) : null;

        // URL 생성
        String url = (String) item.get("url");
        if (url == null || url.isBlank()) {
            url = (String) item.get("website");
            if (url == null || url.isBlank()) {
                url = "https://www.producthunt.com";
            }
        }

        // 제목 생성
        String title = name;
        String tagline = (String) item.get("tagline");
        if (tagline != null && !tagline.isBlank()) {
            title = name + " - " + tagline;
        }

        // 설명 생성
        String description = (String) item.get("description");
        if (description == null || description.isBlank()) {
            description = tagline != null ? tagline : "";
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        ContestCreateRequest.ContestMetadataRequest metadata = ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName("ProductHunt API")
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(PRODUCTHUNT_SOURCE_ID)
            .title(title)
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
     * ProductHunt post 객체에서 태그 추출
     */
    @SuppressWarnings("unchecked")
    private List<String> extractTags(Map<String, Object> item) {
        List<String> tags = new ArrayList<>();
        
        Object topicsObj = item.get("topics");
        if (topicsObj instanceof Map) {
            Map<String, Object> topics = (Map<String, Object>) topicsObj;
            Object edgesObj = topics.get("edges");
            if (edgesObj instanceof List) {
                List<Map<String, Object>> edges = (List<Map<String, Object>>) edgesObj;
                for (Map<String, Object> edge : edges) {
                    Object nodeObj = edge.get("node");
                    if (nodeObj instanceof Map) {
                        Map<String, Object> node = (Map<String, Object>) nodeObj;
                        String topicName = (String) node.get("name");
                        if (topicName != null && !topicName.isBlank()) {
                            tags.add(topicName);
                        }
                    }
                }
            }
        }
        
        return tags;
    }
}
