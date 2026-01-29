package com.tech.n.ai.client.scraper.scraper;

import com.tech.n.ai.client.scraper.config.ScraperProperties;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import com.tech.n.ai.client.scraper.exception.ScrapingException;
import com.tech.n.ai.client.scraper.util.RobotsTxtChecker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MLH (Major League Hacking) 웹 스크래퍼
 * HTML 스크래핑을 통해 학생 해커톤 정보 수집
 */
@Component
@Slf4j
public class MLHScraper implements WebScraper {
    
    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScraperProperties properties;
    private final RetryRegistry retryRegistry;
    
    public MLHScraper(
            @Qualifier("scraperWebClientBuilder") WebClient.Builder webClientBuilder,
            RobotsTxtChecker robotsTxtChecker,
            ScraperProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.robotsTxtChecker = robotsTxtChecker;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }
    
    private static final String EVENTS_PAGE = "/events";
    
    @Override
    public List<ScrapedContestItem> scrape() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get("mlh");
        if (config == null) {
            throw new ScrapingException("MLH scraper source configuration not found");
        }
        
        // robots.txt 확인
        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), EVENTS_PAGE)) {
            throw new ScrapingException("Scraping not allowed by robots.txt for MLH");
        }
        
        WebClient webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        Retry retry = retryRegistry.retry("scraperRetry");
        
        return retry.executeSupplier(() -> {
            try {
                log.debug("Scraping MLH events page: {}", config.getBaseUrl() + EVENTS_PAGE);
                
                String html = webClient.get()
                    .uri(EVENTS_PAGE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                if (html == null || html.isEmpty()) {
                    throw new ScrapingException("Empty HTML content received from MLH");
                }
                
                Document doc = Jsoup.parse(html);
                List<ScrapedContestItem> items = new ArrayList<>();
                
                // MLH events 페이지 구조에 맞는 선택자 사용
                // 실제 구조에 따라 선택자 조정 필요
                var eventElements = doc.select(".event-card, .event-tile, .hackathon-card, [data-event-id]");
                
                if (eventElements.isEmpty()) {
                    // 대안 선택자 시도
                    eventElements = doc.select("a[href*='/events/'], .card, .tile, article");
                }
                
                for (Element element : eventElements) {
                    try {
                        ScrapedContestItem item = extractEventInfo(element, config.getBaseUrl());
                        if (item != null) {
                            items.add(item);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract event info from element: {}", e.getMessage());
                    }
                }
                
                log.debug("Successfully scraped {} events from MLH", items.size());
                return items;
            } catch (WebClientException e) {
                log.error("Failed to fetch MLH events", e);
                throw new ScrapingException("MLH scraping failed", e);
            } catch (Exception e) {
                log.error("Failed to scrape MLH events", e);
                throw new ScrapingException("MLH scraping failed", e);
            }
        });
    }
    
    /**
     * HTML 요소에서 이벤트 정보 추출
     */
    private ScrapedContestItem extractEventInfo(Element element, String baseUrl) {
        try {
            String title = element.select("h2, h3, h4, .title, .event-title, .hackathon-title").first() != null
                ? element.select("h2, h3, h4, .title, .event-title, .hackathon-title").first().text()
                : element.text();
            
            String url = element.attr("href");
            if (url != null && !url.isEmpty() && !url.startsWith("http")) {
                url = baseUrl + (url.startsWith("/") ? url : "/" + url);
            } else if (url == null || url.isEmpty()) {
                // 링크가 없는 경우 부모 요소에서 찾기
                Element linkElement = element.select("a").first();
                if (linkElement != null) {
                    url = linkElement.attr("href");
                    if (url != null && !url.isEmpty() && !url.startsWith("http")) {
                        url = baseUrl + (url.startsWith("/") ? url : "/" + url);
                    }
                } else {
                    url = baseUrl + EVENTS_PAGE;
                }
            }
            
            String description = element.select(".description, .event-description, .summary, p").first() != null
                ? element.select(".description, .event-description, .summary, p").first().text()
                : null;
            
            // 날짜 파싱 (실제 구조에 따라 조정 필요)
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            Element dateElement = element.select(".date, .time, .start-date, .end-date, .event-date").first();
            if (dateElement != null) {
                try {
                    String dateText = dateElement.text();
                    // 간단한 날짜 파싱 (실제 형식에 맞게 조정 필요)
                    // startDate = LocalDateTime.parse(dateText, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    log.debug("Failed to parse date: {}", e.getMessage());
                }
            }
            
            // 이미지 URL 추출
            String imageUrl = element.select("img").first() != null
                ? element.select("img").first().attr("src")
                : null;
            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("http")) {
                imageUrl = baseUrl + (imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl);
            }
            
            String organizer = element.select(".organizer, .host, .university").first() != null
                ? element.select(".organizer, .host, .university").first().text()
                : "MLH";
            String location = element.select(".location, .venue, .city").first() != null
                ? element.select(".location, .venue, .city").first().text()
                : "Online";
            String category = "Student Hackathon";
            
            return ScrapedContestItem.builder()
                .title(title)
                .url(url)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .organizer(organizer)
                .location(location)
                .category(category)
                .prize(null)
                .imageUrl(imageUrl)
                .build();
        } catch (Exception e) {
            log.warn("Failed to extract event info: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getSourceName() {
        return "MLH";
    }
    
    @Override
    public String getBaseUrl() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get("mlh");
        return config != null ? config.getBaseUrl() : null;
    }
}
