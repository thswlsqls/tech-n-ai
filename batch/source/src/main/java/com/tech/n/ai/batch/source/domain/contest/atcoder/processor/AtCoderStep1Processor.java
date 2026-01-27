package com.tech.n.ai.batch.source.domain.contest.atcoder.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
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
public class AtCoderStep1Processor implements ItemProcessor<ScrapedContestItem, ContestCreateRequest> {

    private static final String SOURCE_NAME = "AtCoder";
    
    private final String sourceId;

    @Override
    public @Nullable ContestCreateRequest process(ScrapedContestItem item) throws Exception {
        if (!isValidItem(item)) {
            return null;
        }
        
        return buildContestCreateRequest(item);
    }

    private boolean isValidItem(ScrapedContestItem item) {
        if (item == null) {
            log.warn("Item is null");
            return false;
        }
        
        if (isBlank(item.title())) {
            log.warn("Item title is blank: {}", item.url());
            return false;
        }
        
        if (item.startDate() == null) {
            log.warn("Item startDate is null: {}", item.title());
            return false;
        }
        
        if (item.endDate() == null) {
            log.warn("Item endDate is null: {}", item.title());
            return false;
        }
        
        if (isBlank(item.url())) {
            log.warn("Item url is blank: {}", item.title());
            return false;
        }
        
        return true;
    }

    private ContestCreateRequest buildContestCreateRequest(ScrapedContestItem item) {
        return ContestCreateRequest.builder()
            .sourceId(sourceId)
            .title(item.title())
            .startDate(item.startDate())
            .endDate(item.endDate())
            .description(trimOrEmpty(item.description()))
            .url(item.url())
            .metadata(createMetadata(item))
            .build();
    }

    private ContestCreateRequest.ContestMetadataRequest createMetadata(ScrapedContestItem item) {
        return ContestCreateRequest.ContestMetadataRequest.builder()
            .sourceName(SOURCE_NAME)
            .prize(item.prize())
            .tags(extractTags(item))
            .build();
    }

    private List<String> extractTags(ScrapedContestItem item) {
        List<String> tags = new ArrayList<>();
        
        String category = trimOrEmpty(item.category());
        if (!category.isEmpty()) {
            tags.add(category);
        }
        
        String location = trimOrEmpty(item.location());
        if (!location.isEmpty()) {
            tags.add(location);
        }
        
        String organizer = trimOrEmpty(item.organizer());
        if (!organizer.isEmpty()) {
            tags.add(organizer);
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
