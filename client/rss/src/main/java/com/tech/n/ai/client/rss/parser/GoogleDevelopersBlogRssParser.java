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
 * Google Developers Blog RSS 피드 파서
 * Atom 1.0 형식의 Google Developers Blog 피드를 파싱
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GoogleDevelopersBlogRssParser implements RssParser {
    
    private final WebClient.Builder webClientBuilder;
    private final RssProperties properties;
    private final RetryRegistry retryRegistry;
    
    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("google-developers-blog");
        if (config == null) {
            throw new RssParsingException("Google Developers Blog RSS source configuration not found");
        }
        
        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        Retry retry = retryRegistry.retry("rssRetry");
        
        return retry.executeSupplier(() -> {
            try {
                log.debug("Fetching Google Developers Blog RSS feed from: {}", config.getFeedUrl());
                String feedContent = webClient.get()
                    .uri("")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                if (feedContent == null || feedContent.isEmpty()) {
                    throw new RssParsingException("Empty RSS feed content received from Google Developers Blog");
                }
                
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new StringReader(feedContent));
                
                log.debug("Successfully parsed Google Developers Blog RSS feed. Found {} entries", feed.getEntries().size());
                
                return feed.getEntries().stream()
                    .map(this::convertToRssFeedItem)
                    .collect(Collectors.toList());
            } catch (WebClientException e) {
                log.error("Failed to fetch Google Developers Blog RSS feed", e);
                throw new RssParsingException("Google Developers Blog RSS feed fetch failed", e);
            } catch (Exception e) {
                log.error("Failed to parse Google Developers Blog RSS feed", e);
                throw new RssParsingException("Google Developers Blog RSS parsing failed", e);
            }
        });
    }
    
    /**
     * SyndEntry를 RssFeedItem으로 변환
     * Rome 라이브러리가 Atom 1.0과 RSS 2.0을 모두 SyndEntry로 추상화하므로 동일한 로직 사용
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
            .imageUrl(null) // Atom 1.0 doesn't have direct image URL, would need to parse from description
            .build();
    }
    
    @Override
    public String getSourceName() {
        return "Google Developers Blog";
    }
    
    @Override
    public String getFeedUrl() {
        RssProperties.RssSourceConfig config = properties.getSources().get("google-developers-blog");
        return config != null ? config.getFeedUrl() : null;
    }
}
