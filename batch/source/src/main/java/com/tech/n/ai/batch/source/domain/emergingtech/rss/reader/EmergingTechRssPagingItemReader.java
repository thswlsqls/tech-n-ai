package com.tech.n.ai.batch.source.domain.emergingtech.rss.reader;

import com.tech.n.ai.batch.source.domain.emergingtech.rss.service.EmergingTechRssService;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Emerging Tech RSS 페이징 Item Reader
 * OpenAI + Google AI Blog RSS를 수집하여 페이징 처리
 */
@Slf4j
public class EmergingTechRssPagingItemReader extends AbstractPagingItemReader<RssFeedItem> {

    private final EmergingTechRssService rssService;
    private List<RssFeedItem> allItems;

    public EmergingTechRssPagingItemReader(int pageSize, EmergingTechRssService rssService) {
        setPageSize(pageSize);
        this.rssService = rssService;
    }

    @Override
    protected void doReadPage() {
        initResults();
        fetchAndCacheIfNeeded();
        addPageItemsToResults();
    }

    @Override
    protected void doOpen() {
        log.info("Opening Emerging Tech RSS reader");
    }

    @Override
    protected void doClose() {
        log.info("Closing Emerging Tech RSS reader");
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
            allItems = rssService.fetchAllEmergingTechFeeds();
            log.info("Total RSS items fetched: {}", allItems.size());
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

        List<RssFeedItem> pageItems = allItems.subList(fromIndex, toIndex);
        results.addAll(pageItems);

        log.debug("Page {}: reading items {} to {} (count: {})", page, fromIndex, toIndex - 1, pageItems.size());
    }
}
