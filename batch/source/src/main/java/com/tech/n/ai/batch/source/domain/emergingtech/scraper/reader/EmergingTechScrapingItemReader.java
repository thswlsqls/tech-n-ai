package com.tech.n.ai.batch.source.domain.emergingtech.scraper.reader;

import com.tech.n.ai.batch.source.domain.emergingtech.scraper.service.EmergingTechScraperService;
import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Emerging Tech 웹 크롤링 페이징 Item Reader
 * Anthropic + Meta AI + xAI 기술 블로그를 수집하여 페이징 처리
 */
@Slf4j
public class EmergingTechScrapingItemReader extends AbstractPagingItemReader<ScrapedTechArticle> {

    private final EmergingTechScraperService scraperService;
    private List<ScrapedTechArticle> allItems;

    public EmergingTechScrapingItemReader(int pageSize, EmergingTechScraperService scraperService) {
        setPageSize(pageSize);
        this.scraperService = scraperService;
    }

    @Override
    protected void doReadPage() {
        initResults();
        fetchAndCacheIfNeeded();
        addPageItemsToResults();
    }

    @Override
    protected void doOpen() {
        log.info("Opening Emerging Tech Scraper reader");
    }

    @Override
    protected void doClose() {
        log.info("Closing Emerging Tech Scraper reader");
        allItems = null;
    }

    private void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    private void fetchAndCacheIfNeeded() {
        if (allItems == null) {
            allItems = scraperService.scrapeAllSources();
            log.info("Total scraped articles: {}", allItems.size());
        }
    }

    private void addPageItemsToResults() {
        int page = getPage();
        int pageSize = getPageSize();
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allItems.size());

        if (fromIndex >= allItems.size()) {
            return;
        }

        List<ScrapedTechArticle> pageItems = allItems.subList(fromIndex, toIndex);
        results.addAll(pageItems);

        log.debug("Page {}: reading items {} to {} (count: {})", page, fromIndex, toIndex - 1, pageItems.size());
    }
}
