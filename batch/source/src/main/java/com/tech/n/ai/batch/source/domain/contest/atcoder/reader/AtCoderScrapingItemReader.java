package com.tech.n.ai.batch.source.domain.contest.atcoder.reader;

import com.tech.n.ai.batch.source.domain.contest.atcoder.service.ContestAtCoderScraperService;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class AtCoderScrapingItemReader<T> extends AbstractPagingItemReader<T> {

    private final ContestAtCoderScraperService service;
    private List<ScrapedContestItem> cachedItems;

    public AtCoderScrapingItemReader(int pageSize, ContestAtCoderScraperService service) {
        setPageSize(pageSize);
        this.service = service;
    }

    @Override
    protected void doReadPage() {
        initResults();
        fetchAndCacheIfNeeded();
        addPageItemsToResults();
    }

    @Override
    protected void doOpen() {
        log.info("Opening AtCoder scraping reader with pageSize: {}", getPageSize());
    }

    @Override
    protected void doClose() {
        log.info("Closing AtCoder scraping reader");
        cachedItems = null;
    }

    private void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    private void fetchAndCacheIfNeeded() {
        if (cachedItems == null) {
            cachedItems = service.getScrapedItems();
            log.info("Fetched {} items from AtCoder scraper", cachedItems.size());
        }
    }

    private void addPageItemsToResults() {
        int page = getPage();
        int pageSize = getPageSize();
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, cachedItems.size());

        if (fromIndex >= cachedItems.size()) {
            return;
        }

        List<ScrapedContestItem> pageItems = cachedItems.subList(fromIndex, toIndex);
        for (ScrapedContestItem item : pageItems) {
            results.add((T) item);
        }

        log.debug("Page {}: reading items {} to {} (count: {})", page, fromIndex, toIndex - 1, pageItems.size());
    }
}
