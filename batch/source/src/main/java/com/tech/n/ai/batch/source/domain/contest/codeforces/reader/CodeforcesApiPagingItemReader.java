package com.tech.n.ai.batch.source.domain.contest.codeforces.reader;

import com.tech.n.ai.batch.source.domain.contest.codeforces.service.CodeforcesApiService;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.Contest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class CodeforcesApiPagingItemReader extends AbstractPagingItemReader<Contest> {

    private final CodeforcesApiService apiService;
    private final boolean includeGym;
    private List<Contest> cachedContests;

    public CodeforcesApiPagingItemReader(int pageSize, CodeforcesApiService apiService, boolean includeGym) {
        setPageSize(pageSize);
        this.apiService = apiService;
        this.includeGym = includeGym;
    }

    @Override
    protected void doReadPage() {
        initResults();
        fetchAndCacheIfNeeded();
        addPageItemsToResults();
    }

    @Override
    protected void doOpen() {
        log.info("Opening Codeforces API reader with pageSize: {}", getPageSize());
    }

    @Override
    protected void doClose() {
        log.info("Closing Codeforces API reader");
        cachedContests = null;
    }

    private void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    private void fetchAndCacheIfNeeded() {
        if (cachedContests == null) {
            cachedContests = apiService.getContestList(includeGym);
            log.info("Fetched {} contests from Codeforces API", cachedContests.size());
        }
    }

    private void addPageItemsToResults() {
        int page = getPage();
        int pageSize = getPageSize();
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, cachedContests.size());

        if (fromIndex >= cachedContests.size()) {
            return;
        }

        List<Contest> pageItems = cachedContests.subList(fromIndex, toIndex);
        results.addAll(pageItems);

        log.debug("Page {}: reading items {} to {} (count: {})", page, fromIndex, toIndex - 1, pageItems.size());
    }
}
