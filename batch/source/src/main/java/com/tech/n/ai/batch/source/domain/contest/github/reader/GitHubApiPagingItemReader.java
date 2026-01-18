package com.tech.n.ai.batch.source.domain.contest.github.reader;

import com.tech.n.ai.batch.source.domain.contest.github.service.GitHubApiService;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.Event;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class GitHubApiPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected GitHubApiService service;
    protected Integer perPage;
    protected Integer page;

    public GitHubApiPagingItemReader(int pageSize
                                   , GitHubApiService service
                                   , Integer perPage
                                   , Integer page) {
        setPageSize(pageSize);
        this.service = service;
        this.perPage = perPage;
        this.page = page;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // JobParameter에서 받은 값을 우선 사용, 없으면 기본값 사용
        int currentPage = (this.page != null) ? this.page : (getPage() >= 0 ? getPage() : 1);
        int currentPerPage = (this.perPage != null) ? this.perPage : getPageSize();
        
        log.info("doReadPage ... page: {}, perPage: {}", currentPage, currentPerPage);

        List<Event> itemList = service.getEvents(currentPerPage, currentPage);

        if (itemList != null) {
            for (Event item : itemList) {
                results.add((T) item);
            }
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
    }

    @Override
    protected void doClose() throws Exception {
        log.info("doClose ... ");
    }
}
