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
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AtCoderScraper implements WebScraper {

    private static final String SOURCE_NAME = "AtCoder";
    private static final String CONTESTS_PATH = "/contests";
    private static final String DEFAULT_ORGANIZER = "AtCoder";
    private static final String DEFAULT_LOCATION = "Online";
    private static final String DEFAULT_CATEGORY = "Algorithm Contest";
    private static final String CONFIG_KEY = "atcoder";
    
    private static final String PRIMARY_CONTEST_SELECTOR = ".contest-card, .contest-item, .contest-row, [data-contest-id]";
    private static final String FALLBACK_CONTEST_SELECTOR = "a[href*='/contests/'], table tbody tr, .table tbody tr";
    private static final String TITLE_SELECTOR = "h2, h3, h4, .title, .contest-title, td";
    private static final String DESCRIPTION_SELECTOR = ".description, .contest-description, .summary, p";
    private static final String DATE_SELECTOR = ".date, .time, .start-time, .end-time, td";
    private static final String LINK_SELECTOR = "a";
    
    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScraperProperties properties;
    private final RetryRegistry retryRegistry;

    public AtCoderScraper(
            @Qualifier("scraperWebClientBuilder") WebClient.Builder webClientBuilder,
            RobotsTxtChecker robotsTxtChecker,
            ScraperProperties properties,
            RetryRegistry retryRegistry) {
        this.webClientBuilder = webClientBuilder;
        this.robotsTxtChecker = robotsTxtChecker;
        this.properties = properties;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public List<ScrapedContestItem> scrape() {
        ScraperProperties.ScraperSourceConfig config = getConfig();
        validateRobotsTxt(config);
        
        WebClient webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        Retry retry = retryRegistry.retry("scraperRetry");
        
        return retry.executeSupplier(() -> scrapeWithRetry(webClient, config));
    }

    private ScraperProperties.ScraperSourceConfig getConfig() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(CONFIG_KEY);
        if (config == null) {
            throw new ScrapingException("AtCoder scraper configuration not found");
        }
        return config;
    }

    private void validateRobotsTxt(ScraperProperties.ScraperSourceConfig config) {
        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), CONTESTS_PATH)) {
            throw new ScrapingException("Scraping not allowed by robots.txt");
        }
    }

    private List<ScrapedContestItem> scrapeWithRetry(WebClient webClient, ScraperProperties.ScraperSourceConfig config) {
        try {
            String html = fetchHtml(webClient, config);
            Document document = Jsoup.parse(html);
            Elements contestElements = selectContestElements(document);
            
            return extractContests(contestElements, config.getBaseUrl());
        } catch (WebClientException e) {
            log.error("Failed to fetch AtCoder contests", e);
            throw new ScrapingException("AtCoder scraping failed", e);
        }
    }

    private String fetchHtml(WebClient webClient, ScraperProperties.ScraperSourceConfig config) {
        log.debug("Scraping AtCoder contests: {}{}", config.getBaseUrl(), CONTESTS_PATH);
        
        String html = webClient.get()
            .uri(CONTESTS_PATH)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        if (html == null || html.isEmpty()) {
            throw new ScrapingException("Empty HTML content received");
        }
        
        return html;
    }

    private Elements selectContestElements(Document document) {
        Elements elements = document.select(PRIMARY_CONTEST_SELECTOR);
        
        if (elements.isEmpty()) {
            elements = document.select(FALLBACK_CONTEST_SELECTOR);
        }
        
        return elements;
    }

    private List<ScrapedContestItem> extractContests(Elements elements, String baseUrl) {
        List<ScrapedContestItem> items = new ArrayList<>();
        
        for (Element element : elements) {
            try {
                ScrapedContestItem item = extractContestInfo(element, baseUrl);
                if (item != null) {
                    items.add(item);
                }
            } catch (Exception e) {
                log.warn("Failed to extract contest: {}", e.getMessage());
            }
        }
        
        log.debug("Scraped {} contests", items.size());
        return items;
    }
    
    private ScrapedContestItem extractContestInfo(Element element, String baseUrl) {
        try {
            String title = extractTitle(element);
            String url = extractUrl(element, baseUrl);
            String description = extractDescription(element);
            LocalDateTime[] dates = extractDates(element);
            
            return ScrapedContestItem.builder()
                .title(title)
                .url(url)
                .description(description)
                .startDate(dates[0])
                .endDate(dates[1])
                .organizer(DEFAULT_ORGANIZER)
                .location(DEFAULT_LOCATION)
                .category(DEFAULT_CATEGORY)
                .prize(null)
                .imageUrl(null)
                .build();
        } catch (Exception e) {
            log.warn("Failed to extract contest: {}", e.getMessage());
            return null;
        }
    }

    private String extractTitle(Element element) {
        Element titleElement = element.selectFirst(TITLE_SELECTOR);
        return titleElement != null ? titleElement.text() : element.text();
    }

    private String extractUrl(Element element, String baseUrl) {
        String href = element.attr("href");
        
        if (isBlank(href)) {
            Element linkElement = element.selectFirst(LINK_SELECTOR);
            if (linkElement != null) {
                href = linkElement.attr("href");
            }
        }
        
        if (isBlank(href)) {
            return baseUrl + CONTESTS_PATH;
        }
        
        return buildAbsoluteUrl(href, baseUrl);
    }

    private String buildAbsoluteUrl(String href, String baseUrl) {
        if (href.startsWith("http")) {
            return href;
        }
        
        String path = href.startsWith("/") ? href : "/" + href;
        return baseUrl + path;
    }

    private String extractDescription(Element element) {
        Element descElement = element.selectFirst(DESCRIPTION_SELECTOR);
        return descElement != null ? descElement.text() : null;
    }

    private LocalDateTime[] extractDates(Element element) {
        Element dateElement = element.selectFirst(DATE_SELECTOR);
        
        if (dateElement != null) {
            try {
                String dateText = dateElement.text();
                log.debug("Date parsing not implemented: {}", dateText);
            } catch (Exception e) {
                log.debug("Failed to parse date: {}", e.getMessage());
            }
        }
        
        return new LocalDateTime[]{null, null};
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
    
    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }
    
    @Override
    public String getBaseUrl() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get(CONFIG_KEY);
        return config != null ? config.getBaseUrl() : null;
    }
}
