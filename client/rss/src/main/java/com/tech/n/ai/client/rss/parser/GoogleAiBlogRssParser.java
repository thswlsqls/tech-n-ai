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
 * Google AI Blog RSS 피드 파서
 * RSS 2.0 형식의 Google AI Blog 피드를 파싱
 * media:content 등 추가 필드에서 이미지 URL 추출
 */
@Component
@Slf4j
public class GoogleAiBlogRssParser implements RssParser {

    private final WebClient.Builder webClientBuilder;
    private final RssProperties properties;
    private final RetryRegistry retryRegistry;

    public GoogleAiBlogRssParser(
            @Qualifier("rssWebClientBuilder") WebClient.Builder webClientBuilder,
            RssProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("google-ai-blog");
        if (config == null) {
            throw new RssParsingException("Google AI Blog RSS source configuration not found");
        }

        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        Retry retry = retryRegistry.retry("rssRetry");

        return retry.executeSupplier(() -> {
            try {
                log.debug("Fetching Google AI Blog RSS feed from: {}", config.getFeedUrl());
                String feedContent = webClient.get()
                    .uri("")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (feedContent == null || feedContent.isEmpty()) {
                    throw new RssParsingException("Empty RSS feed content received from Google AI Blog");
                }

                feedContent = removeBOM(feedContent).trim();

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new StringReader(feedContent));

                log.debug("Successfully parsed Google AI Blog RSS feed. Found {} entries", feed.getEntries().size());

                return feed.getEntries().stream()
                    .map(this::convertToRssFeedItem)
                    .collect(Collectors.toList());
            } catch (WebClientException e) {
                log.error("Failed to fetch Google AI Blog RSS feed", e);
                throw new RssParsingException("Google AI Blog RSS feed fetch failed", e);
            } catch (Exception e) {
                log.error("Failed to parse Google AI Blog RSS feed", e);
                throw new RssParsingException("Google AI Blog RSS parsing failed", e);
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

        // Google RSS: enclosure에서 이미지 URL 추출
        String imageUrl = null;
        if (!entry.getEnclosures().isEmpty()) {
            imageUrl = entry.getEnclosures().get(0).getUrl();
        }

        return RssFeedItem.builder()
            .title(entry.getTitle())
            .link(entry.getLink())
            .description(description)
            .publishedDate(publishedDate)
            .author(entry.getAuthor())
            .category(category)
            .guid(entry.getUri() != null ? entry.getUri() : entry.getLink())
            .imageUrl(imageUrl)
            .build();
    }

    @Override
    public String getSourceName() {
        return "Google AI Blog";
    }

    @Override
    public String getFeedUrl() {
        RssProperties.RssSourceConfig config = properties.getSources().get("google-ai-blog");
        return config != null ? config.getFeedUrl() : null;
    }
}
