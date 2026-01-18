package com.tech.n.ai.batch.source.domain.contest.kaggle.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto.Competition;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Kaggle Step1 Processor
 * KaggleDto.Competition → ContestCreateRequest 변환
 * 
 * Kaggle API 공식 문서 참고:
 * https://www.kaggle.com/docs/api
 * 
 * Competition 객체 필드:
 * - id: Integer (대회 ID)
 * - ref: String (대회 참조)
 * - title: String (대회 제목)
 * - description: String (설명)
 * - category: String (카테고리)
 * - reward: String (상금)
 * - teamCount: Integer (팀 수)
 * - deadline: String (제출 마감일, datetime 형식)
 * - enabledDate: String (활성화 날짜, datetime 형식)
 * - submissionDeadline: String (제출 마감일, datetime 형식)
 * - tags: List<String> (태그)
 * - url: String (URL)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class KaggleStep1Processor implements ItemProcessor<Competition, ContestCreateRequest> {

    /**
     * Kaggle 출처의 sourceId
     * TODO: SourcesDocument에서 Kaggle 출처의 ID를 조회하도록 구현 필요
     */
    private static final String KAGGLE_SOURCE_ID = "507f1f77bcf86cd799439013";

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
    };

    @Override
    public @Nullable ContestCreateRequest process(Competition item) throws Exception {
        if (item == null) {
            log.warn("Competition item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.title() == null || item.title().isBlank()) {
            log.warn("Competition title is null or blank, skipping item: {}", item.id());
            return null;
        }

        // 날짜/시간 변환 (enabledDate → startDate, deadline/submissionDeadline → endDate)
        LocalDateTime startDate = parseDateTime(item.enabledDate());
        LocalDateTime endDate = parseDateTime(item.deadline());
        if (endDate == null) {
            endDate = parseDateTime(item.submissionDeadline());
        }

        // URL 생성
        String url = item.url();
        if (url == null || url.isBlank()) {
            if (item.ref() != null) {
                url = "https://www.kaggle.com/competitions/" + item.ref();
            } else {
                url = "https://www.kaggle.com/competitions";
            }
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        ContestCreateRequest.ContestMetadataRequest metadata = ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName("Kaggle API")
            .prize(item.reward())
            .participants(item.teamCount())
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(KAGGLE_SOURCE_ID)
            .title(item.title())
            .startDate(startDate)
            .endDate(endDate)
            .description(item.description() != null ? item.description() : "")
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

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                if (formatter.getZone() != null) {
                    Instant instant = Instant.from(formatter.parse(dateTimeStr));
                    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
                } else {
                    return LocalDateTime.parse(dateTimeStr, formatter);
                }
            } catch (Exception e) {
                // 다음 포맷터 시도
            }
        }

        // ISO 8601 형식으로 직접 파싱 시도
        try {
            Instant instant = Instant.parse(dateTimeStr);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse dateTime: {}, error: {}", dateTimeStr, e.getMessage());
            return null;
        }
    }

    /**
     * Competition 객체에서 태그 추출
     */
    private List<String> extractTags(Competition item) {
        List<String> tags = new ArrayList<>();
        
        if (item.tags() != null) {
            tags.addAll(item.tags());
        }
        
        if (item.category() != null && !item.category().isBlank()) {
            tags.add("category:" + item.category());
        }
        
        return tags;
    }
}
