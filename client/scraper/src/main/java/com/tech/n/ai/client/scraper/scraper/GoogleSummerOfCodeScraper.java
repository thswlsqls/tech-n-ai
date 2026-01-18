package com.tech.n.ai.client.scraper.scraper;

import com.tech.n.ai.client.scraper.config.ScraperProperties;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import com.tech.n.ai.client.scraper.exception.ScrapingException;
import com.tech.n.ai.client.scraper.util.RobotsTxtChecker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Google Summer of Code 웹 스크래퍼
 * HTML 스크래핑을 통해 프로그램 정보 수집
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GoogleSummerOfCodeScraper implements WebScraper {
    
    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScraperProperties properties;
    private final RetryRegistry retryRegistry;
    
    private static final String PROGRAMS_PAGE = "/programs";
    
    @Override
    public List<ScrapedContestItem> scrape() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get("google-summer-of-code");
        if (config == null) {
            throw new ScrapingException("Google Summer of Code scraper source configuration not found");
        }
        
        // robots.txt 확인
        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), PROGRAMS_PAGE)) {
            throw new ScrapingException("Scraping not allowed by robots.txt for Google Summer of Code");
        }
        
        WebClient webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        Retry retry = retryRegistry.retry("scraperRetry");
        
        return retry.executeSupplier(() -> {
            try {
                log.debug("Scraping Google Summer of Code programs page: {}", config.getBaseUrl() + PROGRAMS_PAGE);
                
                String html = webClient.get()
                    .uri(PROGRAMS_PAGE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                if (html == null || html.isEmpty()) {
                    throw new ScrapingException("Empty HTML content received from Google Summer of Code");
                }
                
                Document doc = Jsoup.parse(html);
                List<ScrapedContestItem> items = new ArrayList<>();
                
                // Google Summer of Code 페이지 구조에 맞는 선택자 사용
                // 실제 구조에 따라 선택자 조정 필요
                var programElements = doc.select(".program-card, .program-item, [data-program-id], .gsoc-program");
                
                if (programElements.isEmpty()) {
                    // 대안 선택자 시도
                    programElements = doc.select("a[href*='/programs/'], .card, article");
                }
                
                for (Element element : programElements) {
                    try {
                        ScrapedContestItem item = extractProgramInfo(element, config.getBaseUrl());
                        if (item != null) {
                            items.add(item);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract program info from element: {}", e.getMessage());
                    }
                }
                
                log.debug("Successfully scraped {} programs from Google Summer of Code", items.size());
                return items;
            } catch (WebClientException e) {
                log.error("Failed to fetch Google Summer of Code programs", e);
                throw new ScrapingException("Google Summer of Code scraping failed", e);
            } catch (Exception e) {
                log.error("Failed to scrape Google Summer of Code programs", e);
                throw new ScrapingException("Google Summer of Code scraping failed", e);
            }
        });
    }
    
    /**
     * HTML 요소에서 프로그램 정보 추출
     */
    private ScrapedContestItem extractProgramInfo(Element element, String baseUrl) {
        try {
            String title = element.select("h2, h3, h4, .title, .program-title, .name").first() != null
                ? element.select("h2, h3, h4, .title, .program-title, .name").first().text()
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
                    url = baseUrl + PROGRAMS_PAGE;
                }
            }
            
            String description = element.select(".description, .program-description, .summary, p").first() != null
                ? element.select(".description, .program-description, .summary, p").first().text()
                : null;
            
            // 날짜 파싱 (실제 구조에 따라 조정 필요)
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            Element dateElement = element.select(".date, .time, .start-date, .end-date, .period").first();
            if (dateElement != null) {
                try {
                    String dateText = dateElement.text();
                    // 간단한 날짜 파싱 (실제 형식에 맞게 조정 필요)
                    // startDate = LocalDateTime.parse(dateText, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    log.debug("Failed to parse date: {}", e.getMessage());
                }
            }
            
            String organizer = "Google";
            String location = "Online";
            String category = "Open Source Program";
            
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
                .imageUrl(null)
                .build();
        } catch (Exception e) {
            log.warn("Failed to extract program info: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getSourceName() {
        return "Google Summer of Code";
    }
    
    @Override
    public String getBaseUrl() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get("google-summer-of-code");
        return config != null ? config.getBaseUrl() : null;
    }
}
