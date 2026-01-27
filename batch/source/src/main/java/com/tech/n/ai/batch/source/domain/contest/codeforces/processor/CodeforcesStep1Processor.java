package com.tech.n.ai.batch.source.domain.contest.codeforces.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.Contest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class CodeforcesStep1Processor implements ItemProcessor<Contest, ContestCreateRequest> {

    private static final String SOURCE_NAME = "Codeforces API";
    private static final String CONTESTS_PATH = "/contests";
    private static final String PHASE_FINISHED = "FINISHED";
    private static final String BASE_URL = "https://codeforces.com";

    private final String sourceId;

    @Override
    public @Nullable ContestCreateRequest process(Contest item) throws Exception {
        if (!isValidItem(item)) {
            return null;
        }
        
        return buildContestCreateRequest(item);
    }

    private boolean isValidItem(Contest item) {
        if (item == null) {
            log.warn("Item is null");
            return false;
        }
        
        if (isBlank(item.name())) {
            log.warn("Item name is blank: {}", item.id());
            return false;
        }
        
        if (isFinished(item)) {
            log.debug("Skipping finished contest: {} (phase: {})", item.name(), item.phase());
            return false;
        }
        
        return true;
    }

    private boolean isFinished(Contest item) {
        return PHASE_FINISHED.equals(item.phase());
    }

    private ContestCreateRequest buildContestCreateRequest(Contest item) {
        return ContestCreateRequest.builder()
            .sourceId(sourceId)
            .title(item.name())
            .startDate(toStartDate(item))
            .endDate(toEndDate(item))
            .description(trimOrEmpty(item.description()))
            .url(buildUrl(item))
            .metadata(createMetadata(item))
            .build();
    }

    private ContestCreateRequest.ContestMetadataRequest createMetadata(Contest item) {
        return ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName(SOURCE_NAME)
            .tags(extractTags(item))
            .build();
    }

    private LocalDateTime toStartDate(Contest item) {
        if (item.startTimeSeconds() == null) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(item.startTimeSeconds(), 0, ZoneOffset.UTC);
    }

    private LocalDateTime toEndDate(Contest item) {
        if (item.startTimeSeconds() == null || item.durationSeconds() == null) {
            return null;
        }
        long endTimeSeconds = item.startTimeSeconds() + item.durationSeconds();
        return LocalDateTime.ofEpochSecond(endTimeSeconds, 0, ZoneOffset.UTC);
    }

    private String buildUrl(Contest item) {
        if (!isBlank(item.websiteUrl())) {
            return item.websiteUrl();
        }
        
        if (item.id() != null) {
            return BASE_URL + CONTESTS_PATH + "/" + item.id();
        }
        
        return BASE_URL + CONTESTS_PATH;
    }

    private List<String> extractTags(Contest item) {
        List<String> tags = new ArrayList<>();
        
        String type = trimOrEmpty(item.type());
        if (!type.isEmpty()) {
            tags.add(type);
        }
        
        String kind = trimOrEmpty(item.kind());
        if (!kind.isEmpty()) {
            tags.add(kind);
        }
        
        String phase = trimOrEmpty(item.phase());
        if (!phase.isEmpty()) {
            tags.add(phase);
        }
        
        return tags;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }
}

