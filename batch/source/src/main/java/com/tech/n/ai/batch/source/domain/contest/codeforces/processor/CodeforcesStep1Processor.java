package com.tech.n.ai.batch.source.domain.contest.codeforces.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.Contest;
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
 * Codeforces Step1 Processor
 * CodeforcesDto.Contest → ContestCreateRequest 변환
 * 
 * Codeforces API 공식 문서 참고:
 * https://codeforces.com/apiHelp
 * 
 * Contest 객체 필드:
 * - id: Integer (대회 ID)
 * - name: String (대회명)
 * - type: String (대회 타입: "CF", "ICPC" 등)
 * - phase: String (대회 단계: "BEFORE", "CODING", "FINISHED" 등)
 * - startTimeSeconds: Long (시작 시간, Unix timestamp)
 * - durationSeconds: Integer (지속 시간, 초)
 * - websiteUrl: String (웹사이트 URL)
 * - description: String (설명)
 * - kind: String (대회 종류)
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class CodeforcesStep1Processor implements ItemProcessor<Contest, ContestCreateRequest> {

    /**
     * Codeforces 출처의 sourceId
     * TODO: SourcesDocument에서 Codeforces 출처의 ID를 조회하도록 구현 필요
     */
    private static final String CODEFORCES_SOURCE_ID = "507f1f77bcf86cd799439011";

    @Override
    public @Nullable ContestCreateRequest process(Contest item) throws Exception {
        if (item == null) {
            log.warn("Contest item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.name() == null || item.name().isBlank()) {
            log.warn("Contest name is null or blank, skipping item: {}", item.id());
            return null;
        }

        // 날짜/시간 변환 (Unix timestamp → LocalDateTime)
        LocalDateTime startDate = null;
        if (item.startTimeSeconds() != null) {
            startDate = LocalDateTime.ofEpochSecond(item.startTimeSeconds(), 0, ZoneOffset.UTC);
        }

        LocalDateTime endDate = null;
        if (item.startTimeSeconds() != null && item.durationSeconds() != null) {
            endDate = LocalDateTime.ofEpochSecond(
                item.startTimeSeconds() + item.durationSeconds(), 0, ZoneOffset.UTC);
        }

        // URL 생성 (websiteUrl이 없으면 기본 URL 사용)
        String url = item.websiteUrl();
        if (url == null || url.isBlank()) {
            if (item.id() != null) {
                url = "https://codeforces.com/contests/" + item.id();
            } else {
                url = "https://codeforces.com/contests";
            }
        }

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        ContestCreateRequest.ContestMetadataRequest metadata = ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName("Codeforces API")
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(CODEFORCES_SOURCE_ID)
            .title(item.name())
            .startDate(startDate)
            .endDate(endDate)
            .description(item.description() != null ? item.description() : "")
            .url(url)
            .metadata(metadata)
            .build();
    }

    /**
     * Contest 객체에서 태그 추출
     * type, kind, phase 등에서 태그 추출
     */
    private List<String> extractTags(Contest item) {
        List<String> tags = new ArrayList<>();
        
        if (item.type() != null && !item.type().isBlank()) {
            tags.add(item.type());
        }
        
        if (item.kind() != null && !item.kind().isBlank()) {
            tags.add(item.kind());
        }
        
        if (item.phase() != null && !item.phase().isBlank()) {
            tags.add(item.phase());
        }
        
        return tags;
    }
}
