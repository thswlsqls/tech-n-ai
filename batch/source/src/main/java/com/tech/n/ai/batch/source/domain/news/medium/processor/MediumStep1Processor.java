package com.tech.n.ai.batch.source.domain.news.medium.processor;

import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class MediumStep1Processor implements ItemProcessor<RssFeedItem, NewsCreateRequest> {

    private static final String SOURCE_NAME = "Medium Technology RSS";
    private static final int SUMMARY_MAX_LENGTH = 500;

    private final String sourceId;

    @Override
    public @Nullable NewsCreateRequest process(RssFeedItem item) throws Exception {
        if (!isValidItem(item)) {
            return null;
        }

        return buildNewsCreateRequest(item);
    }

    private boolean isValidItem(RssFeedItem item) {
        if (item == null) {
            log.warn("Item is null");
            return false;
        }

        if (isBlank(item.title())) {
            log.warn("Item title is blank: {}", item.guid());
            return false;
        }

        if (isBlank(item.link())) {
            log.warn("Item link is blank: {}", item.title());
            return false;
        }

        return true;
    }

    private NewsCreateRequest buildNewsCreateRequest(RssFeedItem item) {
        return NewsCreateRequest.builder()
            .sourceId(sourceId)
            .title(trimOrEmpty(item.title()))
            .content(trimOrEmpty(item.description()))
            .summary(createSummary(item.description()))
            .publishedAt(item.publishedDate())
            .url(trimOrEmpty(item.link()))
            .author(trimOrEmpty(item.author()))
            .metadata(createMetadata(item))
            .build();
    }

    private NewsCreateRequest.NewsMetadataRequest createMetadata(RssFeedItem item) {
        return NewsCreateRequest.NewsMetadataRequest.builder()
            .sourceName(SOURCE_NAME)
            .tags(extractTags(item))
            .build();
    }

    private String createSummary(String description) {
        String trimmed = trimOrEmpty(description);
        
        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.length() <= SUMMARY_MAX_LENGTH) {
            return trimmed;
        }

        return trimmed.substring(0, SUMMARY_MAX_LENGTH) + "...";
    }

    private List<String> extractTags(RssFeedItem item) {
        List<String> tags = new ArrayList<>();

        String category = trimOrEmpty(item.category());
        if (!category.isEmpty()) {
            tags.add(category);
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
