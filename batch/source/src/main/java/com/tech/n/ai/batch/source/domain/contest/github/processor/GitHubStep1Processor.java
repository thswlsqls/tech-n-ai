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
import org.springframework.lang.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class GitHubStep1Processor implements ItemProcessor<Event, ContestCreateRequest> {

    private static final String SOURCE_NAME = "GitHub API";
    private static final String BASE_URL = "https://github.com";
    private static final String API_PREFIX = "api.github.com/repos";
    
    private final String sourceId;

    @Override
    public @Nullable ContestCreateRequest process(Event item) throws Exception {
        if (!isValidItem(item)) {
            return null;
        }
        
        return buildContestCreateRequest(item);
    }

    private boolean isValidItem(Event item) {
        if (item == null) {
            log.warn("Item is null");
            return false;
        }
        
        if (isBlank(item.id())) {
            log.warn("Item id is blank");
            return false;
        }
        
        return true;
    }

    private ContestCreateRequest buildContestCreateRequest(Event item) {
        return ContestCreateRequest.builder()
            .sourceId(sourceId)
            .title(buildTitle(item))
            .startDate(toStartDate(item))
            .endDate(toEndDate(item))
            .description(buildDescription(item))
            .url(buildUrl(item))
            .metadata(createMetadata(item))
            .build();
    }

    private ContestCreateRequest.ContestMetadataRequest createMetadata(Event item) {
        return ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName(SOURCE_NAME)
            .tags(extractTags(item))
            .build();
    }

    private LocalDateTime toStartDate(Event item) {
        if (isBlank(item.createdAt())) {
            return null;
        }
        
        try {
            Instant instant = Instant.parse(item.createdAt());
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse createdAt: {}", item.createdAt());
            return null;
        }
    }

    private LocalDateTime toEndDate(Event item) {
        LocalDateTime startDate = toStartDate(item);
        return startDate != null ? startDate.plusDays(1) : null;
    }

    private String buildUrl(Event item) {
        if (item.repo() != null && item.repo().url() != null) {
            return item.repo().url().replace(API_PREFIX, BASE_URL.replace("https://", ""));
        }
        return BASE_URL;
    }

    private String buildTitle(Event item) {
        StringBuilder title = new StringBuilder();
        
        if (!isBlank(item.type())) {
            title.append(item.type());
        }
        
        if (item.repo() != null && !isBlank(item.repo().name())) {
            if (title.length() > 0) {
                title.append(" - ");
            }
            title.append(item.repo().name());
        }
        
        return title.length() > 0 ? title.toString() : "GitHub Event";
    }

    private String buildDescription(Event item) {
        StringBuilder description = new StringBuilder();
        
        if (!isBlank(item.type())) {
            description.append("Event Type: ").append(item.type());
        }
        
        if (item.actor() != null && !isBlank(item.actor().login())) {
            if (description.length() > 0) {
                description.append("\n");
            }
            description.append("Actor: ").append(item.actor().login());
        }
        
        if (item.repo() != null && !isBlank(item.repo().name())) {
            if (description.length() > 0) {
                description.append("\n");
            }
            description.append("Repository: ").append(item.repo().name());
        }
        
        return description.toString();
    }

    private List<String> extractTags(Event item) {
        List<String> tags = new ArrayList<>();
        
        if (!isBlank(item.type())) {
            tags.add(item.type());
        }
        
        if (item.org() != null && !isBlank(item.org().login())) {
            tags.add("org:" + item.org().login());
        }
        
        return tags;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
