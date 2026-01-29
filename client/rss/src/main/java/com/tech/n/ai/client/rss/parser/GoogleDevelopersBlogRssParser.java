package com.tech.n.ai.client.rss.parser;

import com.tech.n.ai.client.rss.config.RssProperties;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.exception.RssParsingException;
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
 * Google Developers Blog RSS 피드 파서
 * Atom 1.0 형식의 Google Developers Blog 피드를 파싱
 */
@Component
@Slf4j
public class GoogleDevelopersBlogRssParser implements RssParser {
    
    private final WebClient.Builder webClientBuilder;
    private final RssProperties properties;
    private final RetryRegistry retryRegistry;
    
    public GoogleDevelopersBlogRssParser(
            @Qualifier("rssWebClientBuilder") WebClient.Builder webClientBuilder,
            RssProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }
    
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
                
                // BOM 및 앞뒤 공백 제거
                feedContent = removeBOM(feedContent).trim();
                
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
     * BOM (Byte Order Mark) 제거
     * UTF-8 BOM(U+FEFF)이 문자열 시작에 있을 경우 제거
     */
    private String removeBOM(String content) {
        if (content != null && content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }
    
    /**
     * SyndEntry를 RssFeedItem으로 변환
     * Rome 라이브러리가 Atom 1.0과 RSS 2.0을 모두 SyndEntry로 추상화하므로 동일한 로직 사용
     * 발행일이 없는 경우 null로 유지 (데이터 무결성 보장)
     */
    private RssFeedItem convertToRssFeedItem(SyndEntry entry) {
        LocalDateTime publishedDate = null;
        
        // 1. entry.getPublishedDate() 우선 사용
        if (entry.getPublishedDate() != null) {
            publishedDate = LocalDateTime.ofInstant(
                entry.getPublishedDate().toInstant(),
                ZoneId.systemDefault()
            );
        }
        // 2. entry.getUpdatedDate() fallback (Atom 피드에서 사용)
        else if (entry.getUpdatedDate() != null) {
            publishedDate = LocalDateTime.ofInstant(
                entry.getUpdatedDate().toInstant(),
                ZoneId.systemDefault()
            );
        }
        // 3. 발행일 정보가 없으면 null 유지 (정확하지 않은 데이터 저장 방지)
        
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
