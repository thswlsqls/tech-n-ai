package com.tech.n.ai.batch.source.domain.contest.github.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.Event;
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
 * GitHub Step1 Processor
 * GitHubDto.Event → ContestCreateRequest 변환
 * 
 * GitHub API 공식 문서 참고:
 * https://docs.github.com/en/rest
 * 
 * Event 객체 필드:
 * - id: String (이벤트 ID)
 * - type: String (이벤트 타입: "WatchEvent", "ForkEvent", "PushEvent" 등)
 * - actor: Actor (이벤트를 발생시킨 사용자)
 * - repo: Repository (저장소 정보)
 * - payload: Map<String, Object> (이벤트별 페이로드)
 * - createdAt: String (생성 시간, ISO 8601 형식)
 * - org: Organization (조직 정보, 선택적)
 * 
 * Note: GitHub Events API는 Contest 정보를 직접 제공하지 않으므로,
 * 이벤트 정보를 기반으로 Contest 정보를 추출합니다.
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class GitHubStep1Processor implements ItemProcessor<Event, ContestCreateRequest> {

    /**
     * GitHub 출처의 sourceId
     * TODO: SourcesDocument에서 GitHub 출처의 ID를 조회하도록 구현 필요
     */
    private static final String GITHUB_SOURCE_ID = "507f1f77bcf86cd799439012";

    @Override
    public @Nullable ContestCreateRequest process(Event item) throws Exception {
        if (item == null) {
            log.warn("Event item is null");
            return null;
        }

        // 필수 필드 검증
        if (item.id() == null || item.id().isBlank()) {
            log.warn("Event id is null or blank, skipping item");
            return null;
        }

        // 날짜/시간 변환 (ISO 8601 → LocalDateTime)
        LocalDateTime startDate = null;
        if (item.createdAt() != null && !item.createdAt().isBlank()) {
            try {
                // GitHub API는 ISO 8601 형식 (예: "2022-06-09T12:47:28Z")
                Instant instant = Instant.parse(item.createdAt());
                startDate = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            } catch (Exception e) {
                log.warn("Failed to parse createdAt: {}, error: {}", item.createdAt(), e.getMessage());
            }
        }

        // endDate는 startDate + 1일로 설정 (GitHub 이벤트는 종료 시간이 없으므로)
        LocalDateTime endDate = startDate != null ? startDate.plusDays(1) : null;

        // URL 생성
        String url = "https://github.com";
        if (item.repo() != null && item.repo().url() != null) {
            url = item.repo().url().replace("api.github.com/repos", "github.com");
        }

        // 제목 생성 (이벤트 타입과 저장소 이름 기반)
        String title = generateTitle(item);

        // 설명 생성
        String description = generateDescription(item);

        // 태그 추출
        List<String> tags = extractTags(item);

        // Metadata 생성
        ContestCreateRequest.ContestMetadataRequest metadata = ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName("GitHub API")
            .tags(tags)
            .build();

        return ContestCreateRequest.builder()
            .sourceId(GITHUB_SOURCE_ID)
            .title(title)
            .startDate(startDate)
            .endDate(endDate)
            .description(description)
            .url(url)
            .metadata(metadata)
            .build();
    }

    /**
     * Event 객체에서 제목 생성
     */
    private String generateTitle(Event item) {
        StringBuilder title = new StringBuilder();
        
        if (item.type() != null) {
            title.append(item.type());
        }
        
        if (item.repo() != null && item.repo().name() != null) {
            if (title.length() > 0) {
                title.append(" - ");
            }
            title.append(item.repo().name());
        }
        
        return title.length() > 0 ? title.toString() : "GitHub Event";
    }

    /**
     * Event 객체에서 설명 생성
     */
    private String generateDescription(Event item) {
        StringBuilder description = new StringBuilder();
        
        if (item.type() != null) {
            description.append("Event Type: ").append(item.type());
        }
        
        if (item.actor() != null && item.actor().login() != null) {
            if (description.length() > 0) {
                description.append("\n");
            }
            description.append("Actor: ").append(item.actor().login());
        }
        
        if (item.repo() != null && item.repo().name() != null) {
            if (description.length() > 0) {
                description.append("\n");
            }
            description.append("Repository: ").append(item.repo().name());
        }
        
        return description.length() > 0 ? description.toString() : "";
    }

    /**
     * Event 객체에서 태그 추출
     */
    private List<String> extractTags(Event item) {
        List<String> tags = new ArrayList<>();
        
        if (item.type() != null && !item.type().isBlank()) {
            tags.add(item.type());
        }
        
        if (item.org() != null && item.org().login() != null) {
            tags.add("org:" + item.org().login());
        }
        
        return tags;
    }
}
