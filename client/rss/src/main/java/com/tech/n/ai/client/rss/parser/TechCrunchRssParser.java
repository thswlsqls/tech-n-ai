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
 * TechCrunch RSS 피드 파서
 * RSS 2.0 형식의 TechCrunch 피드를 파싱
 */
@Component
@Slf4j
public class TechCrunchRssParser implements RssParser {
    
    private final WebClient.Builder webClientBuilder;
    private final RssProperties properties;
    private final RetryRegistry retryRegistry;
    
    public TechCrunchRssParser(
            @Qualifier("rssWebClientBuilder") WebClient.Builder webClientBuilder,
            RssProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }
    
    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("techcrunch");
        if (config == null) {
            throw new RssParsingException("TechCrunch RSS source configuration not found");
        }
        
        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        Retry retry = retryRegistry.retry("rssRetry");
        
        return retry.executeSupplier(() -> {
            try {
                log.debug("Fetching TechCrunch RSS feed from: {}", config.getFeedUrl());
                String feedContent = webClient.get()
                    .uri("")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                if (feedContent == null || feedContent.isEmpty()) {
                    throw new RssParsingException("Empty RSS feed content received from TechCrunch");
                }
                
                // XML 콘텐츠 정규화 (BOM 제거, XML 선언 이전 문자 제거)
                feedContent = normalizeXmlContent(feedContent);
                
                // 첫 번째 문자들의 코드포인트 확인 (디버깅용)
                log.info("Feed content length: {}", feedContent.length());
                log.info("Feed content first 200 chars: [{}]", 
                    feedContent.length() > 200 ? feedContent.substring(0, 200) : feedContent);
                
                // 첫 5개 문자의 코드포인트 출력
                StringBuilder codePoints = new StringBuilder();
                for (int i = 0; i < Math.min(5, feedContent.length()); i++) {
                    codePoints.append(String.format("U+%04X ", (int) feedContent.charAt(i)));
                }
                log.info("First 5 char code points: {}", codePoints.toString());
                
                // XML 선언으로 시작하지 않으면 경고
                if (!feedContent.startsWith("<?xml")) {
                    log.warn("Feed content does NOT start with XML declaration. First char code: U+{}", 
                        String.format("%04X", (int) feedContent.charAt(0)));
                }
                
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new StringReader(feedContent));
                
                log.debug("Successfully parsed TechCrunch RSS feed. Found {} entries", feed.getEntries().size());
                
                return feed.getEntries().stream()
                    .map(this::convertToRssFeedItem)
                    .collect(Collectors.toList());
            } catch (WebClientException e) {
                log.error("Failed to fetch TechCrunch RSS feed", e);
                throw new RssParsingException("TechCrunch RSS feed fetch failed", e);
            } catch (Exception e) {
                log.error("Failed to parse TechCrunch RSS feed", e);
                throw new RssParsingException("TechCrunch RSS parsing failed", e);
            }
        });
    }
    
    /**
     * XML 콘텐츠 정규화
     * - BOM(Byte Order Mark) 제거
     * - XML 선언 이전의 불필요한 문자 제거
     * - 비정상적인 앞부분 문자 정리
     */
    private String normalizeXmlContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        // 1. UTF-8 BOM 제거
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        
        // 2. XML 선언(<?xml) 이전의 모든 문자 제거
        int xmlDeclIndex = content.indexOf("<?xml");
        if (xmlDeclIndex > 0) {
            log.warn("Found {} characters before XML declaration, removing them", xmlDeclIndex);
            content = content.substring(xmlDeclIndex);
        } else if (xmlDeclIndex < 0) {
            // XML 선언이 없으면 <rss 태그를 찾음
            int rssIndex = content.indexOf("<rss");
            if (rssIndex > 0) {
                log.warn("No XML declaration found, but found <rss> at index {}. Removing preceding content.", rssIndex);
                content = content.substring(rssIndex);
            }
        }
        
        // 3. 앞뒤 공백 제거
        return content.trim();
    }
    
    /**
     * SyndEntry를 RssFeedItem으로 변환
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
            .imageUrl(null) // RSS 2.0 doesn't have direct image URL, would need to parse from description
            .build();
    }
    
    @Override
    public String getSourceName() {
        return "TechCrunch";
    }
    
    @Override
    public String getFeedUrl() {
        RssProperties.RssSourceConfig config = properties.getSources().get("techcrunch");
        return config != null ? config.getFeedUrl() : null;
    }
}
