package com.tech.n.ai.batch.source.domain.contest.github.reader;

import com.tech.n.ai.batch.source.domain.contest.github.service.GitHubApiService;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.Event;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class GitHubApiPagingItemReader extends AbstractPagingItemReader<Event> {

    private final GitHubApiService apiService;
    private final Integer perPage;
    private final Integer page;
    private List<Event> cachedEvents;

    public GitHubApiPagingItemReader(int pageSize, GitHubApiService apiService, Integer perPage, Integer page) {
        setPageSize(pageSize);
        this.apiService = apiService;
        this.perPage = perPage;
        this.page = page;
    }

    @Override
    protected void doReadPage() {
        initResults();
        fetchAndCacheIfNeeded();
        addPageItemsToResults();
    }

    @Override
    protected void doOpen() {
        log.info("Opening GitHub API reader with pageSize: {}, perPage: {}, page: {}", 
            getPageSize(), perPage, page);
    }

    @Override
    protected void doClose() {
        log.info("Closing GitHub API reader");
        cachedEvents = null;
    }

    private void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    private void fetchAndCacheIfNeeded() {
        if (cachedEvents == null) {
            cachedEvents = apiService.getEvents(perPage, page);
            log.info("Fetched {} events from GitHub API", cachedEvents.size());
        }
    }

    private void addPageItemsToResults() {
        int currentPage = getPage();
        int pageSize = getPageSize();
        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, cachedEvents.size());

        if (fromIndex >= cachedEvents.size()) {
            return;
        }

        List<Event> pageItems = cachedEvents.subList(fromIndex, toIndex);
        results.addAll(pageItems);

        log.debug("Page {}: reading items {} to {} (count: {})", 
            currentPage, fromIndex, toIndex - 1, pageItems.size());
    }
}
