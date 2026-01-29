package com.tech.n.ai.client.rss.parser;

import com.tech.n.ai.client.rss.config.RssProperties;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.exception.RssParsingException;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI Blog RSS 피드 파서
 * RSS 2.0 형식의 OpenAI Blog 피드를 파싱
 */
@Component
@Slf4j
public class OpenAiBlogRssParser implements RssParser {

    private final WebClient.Builder webClientBuilder;
    private final RssProperties properties;
    private final RetryRegistry retryRegistry;

    public OpenAiBlogRssParser(
            @Qualifier("rssWebClientBuilder") WebClient.Builder webClientBuilder,
            RssProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("openai-blog");
        if (config == null) {
            throw new RssParsingException("OpenAI Blog RSS source configuration not found");
        }

        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        Retry retry = retryRegistry.retry("rssRetry");

        return retry.executeSupplier(() -> {
            try {
                log.debug("Fetching OpenAI Blog RSS feed from: {}", config.getFeedUrl());
                String feedContent = webClient.get()
                    .uri("")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (feedContent == null || feedContent.isEmpty()) {
                    throw new RssParsingException("Empty RSS feed content received from OpenAI Blog");
                }

                feedContent = removeBOM(feedContent).trim();

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new StringReader(feedContent));

                log.debug("Successfully parsed OpenAI Blog RSS feed. Found {} entries", feed.getEntries().size());

                return feed.getEntries().stream()
                    .map(this::convertToRssFeedItem)
                    .collect(Collectors.toList());
            } catch (WebClientException e) {
                log.error("Failed to fetch OpenAI Blog RSS feed", e);
                throw new RssParsingException("OpenAI Blog RSS feed fetch failed", e);
            } catch (Exception e) {
                log.error("Failed to parse OpenAI Blog RSS feed", e);
                throw new RssParsingException("OpenAI Blog RSS parsing failed", e);
            }
        });
    }

    private String removeBOM(String content) {
        if (content != null && content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }

    private RssFeedItem convertToRssFeedItem(SyndEntry entry) {
        LocalDateTime publishedDate = null;

        if (entry.getPublishedDate() != null) {
            publishedDate = LocalDateTime.ofInstant(
                entry.getPublishedDate().toInstant(), ZoneId.systemDefault());
        } else if (entry.getUpdatedDate() != null) {
            publishedDate = LocalDateTime.ofInstant(
                entry.getUpdatedDate().toInstant(), ZoneId.systemDefault());
        }

        String description = entry.getDescription() != null
            ? entry.getDescription().getValue()
            : null;

        String category = entry.getCategories().isEmpty()
            ? null
            : entry.getCategories().stream()
                .map(SyndCategory::getName)
                .collect(Collectors.joining(","));

        String guid = entry.getUri() != null ? entry.getUri() : entry.getLink();

        return RssFeedItem.builder()
            .title(entry.getTitle())
            .link(entry.getLink())
            .description(description)
            .publishedDate(publishedDate)
            .author(entry.getAuthor())
            .category(category)
            .guid(guid)
            .imageUrl(null)
            .build();
    }

    @Override
    public String getSourceName() {
        return "OpenAI Blog";
    }

    @Override
    public String getFeedUrl() {
        RssProperties.RssSourceConfig config = properties.getSources().get("openai-blog");
        return config != null ? config.getFeedUrl() : null;
    }
}
