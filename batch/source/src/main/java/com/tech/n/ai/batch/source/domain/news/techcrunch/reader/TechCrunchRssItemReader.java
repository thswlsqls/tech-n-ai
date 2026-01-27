package com.tech.n.ai.batch.source.domain.news.techcrunch.reader;

import com.tech.n.ai.batch.source.domain.news.techcrunch.service.NewsTechCrunchRssService;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class TechCrunchRssItemReader extends AbstractPagingItemReader<RssFeedItem> {

    private final NewsTechCrunchRssService service;
    private List<RssFeedItem> feedItems;
    private int currentIndex;

    public TechCrunchRssItemReader(int pageSize, NewsTechCrunchRssService service) {
        setPageSize(pageSize);
        this.service = service;
    }

    @Override
    protected void doOpen() throws Exception {
        log.info("Opening reader with pageSize: {}", getPageSize());
        resetState();
    }

    @Override
    protected void doReadPage() {
        initializeResults();

        if (isFirstPage()) {
            fetchFeedItems();
        }

        addItemsToResults();
    }

    @Override
    protected void doClose() throws Exception {
        log.info("Closing reader");
        resetState();
    }

    private void initializeResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    private boolean isFirstPage() {
        return feedItems == null;
    }

    private void fetchFeedItems() {
        log.info("Fetching RSS feed items");
        feedItems = service.getFeedItems();
        currentIndex = 0;
    }

    private void addItemsToResults() {
        if (hasNoMoreItems()) {
            log.info("No more items to read");
            return;
        }

        int startIndex = currentIndex;
        int endIndex = calculateEndIndex();

        for (int i = startIndex; i < endIndex; i++) {
            results.add(feedItems.get(i));
        }

        currentIndex = endIndex;
        logReadProgress(startIndex + 1, endIndex);
    }

    private boolean hasNoMoreItems() {
        return feedItems == null || currentIndex >= feedItems.size();
    }

    private int calculateEndIndex() {
        int totalSize = feedItems != null ? feedItems.size() : 0;
        return Math.min(currentIndex + getPageSize(), totalSize);
    }

    private void logReadProgress(int from, int to) {
        log.info("Read page: {}, items: {} to {}", getPage() + 1, from, to);
    }

    private void resetState() {
        feedItems = null;
        currentIndex = 0;
    }
}
