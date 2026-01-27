package com.tech.n.ai.batch.source.domain.contest.leetcode.reader;

import com.tech.n.ai.batch.source.domain.contest.leetcode.service.ContestLeetCodeScraperService;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class LeetCodeScrapingItemReader<T> extends AbstractPagingItemReader<T> {

    protected ContestLeetCodeScraperService service;
    protected List<ScrapedContestItem> allItems;
    protected int currentIndex = 0;

    public LeetCodeScrapingItemReader(int pageSize, ContestLeetCodeScraperService service) {
        setPageSize(pageSize);
        this.service = service;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // 첫 번째 페이지에서만 스크래핑 수행
        if (allItems == null) {
            log.info("doReadPage ... scraping contest items (first page)");
            allItems = service.getScrapedItems();
            currentIndex = 0;
        }

        // 현재 페이지에 해당하는 아이템들을 추가
        int pageSize = getPageSize();
        int endIndex = Math.min(currentIndex + pageSize, allItems != null ? allItems.size() : 0);
        
        if (allItems != null && currentIndex < allItems.size()) {
            for (int i = currentIndex; i < endIndex; i++) {
                results.add((T) allItems.get(i));
            }
            currentIndex = endIndex;
            log.info("doReadPage ... page: {}, items: {} to {}", getPage(), currentIndex - pageSize, currentIndex - 1);
        } else {
            log.info("doReadPage ... no more items to read");
        }
    }

    protected void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    @Override
    protected void doOpen() throws Exception {
        log.info("doOpen ... ");
        log.info("pageSize : {}", getPageSize());
        allItems = null;
        currentIndex = 0;
    }

    @Override
    protected void doClose() throws Exception {
        log.info("doClose ... ");
        allItems = null;
        currentIndex = 0;
    }
}
