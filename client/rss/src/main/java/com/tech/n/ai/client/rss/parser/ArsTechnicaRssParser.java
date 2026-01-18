package com.tech.n.ai.client.rss.parser;

import com.tech.n.ai.client.rss.config.RssProperties;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.exception.RssParsingException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ars Technica RSS 피드 파서
 * RSS 2.0 형식의 Ars Technica 피드를 파싱
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ArsTechnicaRssParser implements RssParser {
    
    private final WebClient.Builder webClientBuilder;
    private final RssProperties properties;
    private final RetryRegistry retryRegistry;
    
    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("ars-technica");
        if (config == null) {
            throw new RssParsingException("Ars Technica RSS source configuration not found");
        }
        
        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        Retry retry = retryRegistry.retry("rssRetry");
        
        return retry.executeSupplier(() -> {
            try {
                log.debug("Fetching Ars Technica RSS feed from: {}", config.getFeedUrl());
                String feedContent = webClient.get()
                    .uri("")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                if (feedContent == null || feedContent.isEmpty()) {
                    throw new RssParsingException("Empty RSS feed content received from Ars Technica");
                }
                
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new StringReader(feedContent));
                
                log.debug("Successfully parsed Ars Technica RSS feed. Found {} entries", feed.getEntries().size());
                
                return feed.getEntries().stream()
                    .map(this::convertToRssFeedItem)
                    .collect(Collectors.toList());
            } catch (WebClientException e) {
                log.error("Failed to fetch Ars Technica RSS feed", e);
                throw new RssParsingException("Ars Technica RSS feed fetch failed", e);
            } catch (Exception e) {
                log.error("Failed to parse Ars Technica RSS feed", e);
                throw new RssParsingException("Ars Technica RSS parsing failed", e);
            }
        });
    }
    
    /**
     * SyndEntry를 RssFeedItem으로 변환
     */
    private RssFeedItem convertToRssFeedItem(SyndEntry entry) {
        LocalDateTime publishedDate = null;
        if (entry.getPublishedDate() != null) {
            publishedDate = LocalDateTime.ofInstant(
                entry.getPublishedDate().toInstant(),
                ZoneId.systemDefault()
            );
        }
        
        String description = entry.getDescription() != null 
            ? entry.getDescription().getValue() 
            : null;
        
        String author = entry.getAuthor();
        
        String category = entry.getCategories().isEmpty() 
            ? null 
            : entry.getCategories().get(0).getName();
        
        String guid = entry.getUri() != null 
            ? entry.getUri() 
            : entry.getLink();
        
        return RssFeedItem.builder()
            .title(entry.getTitle())
            .link(entry.getLink())
            .description(description)
            .publishedDate(publishedDate)
            .author(author)
            .category(category)
            .guid(guid)
            .imageUrl(null) // RSS 2.0 doesn't have direct image URL, would need to parse from description
            .build();
    }
    
    @Override
    public String getSourceName() {
        return "Ars Technica";
    }
    
    @Override
    public String getFeedUrl() {
        RssProperties.RssSourceConfig config = properties.getSources().get("ars-technica");
        return config != null ? config.getFeedUrl() : null;
    }
}
