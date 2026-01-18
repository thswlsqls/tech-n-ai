package com.tech.n.ai.batch.source.domain.contest.hackernews.reader;

import com.tech.n.ai.batch.source.domain.contest.hackernews.service.HackerNewsApiService;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class HackerNewsApiPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected HackerNewsApiService service;
    protected Integer limit;

    public HackerNewsApiPagingItemReader(int pageSize
                                       , HackerNewsApiService service
                                       , Integer limit) {
        setPageSize(pageSize);
        this.service = service;
        this.limit = limit;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // JobParameter에서 받은 값을 우선 사용, 없으면 기본값 사용
        int currentLimit = (this.limit != null) ? this.limit : getPageSize();
        
        log.info("doReadPage ... limit: {}", currentLimit);

        List<ItemResponse> itemList = service.getTopStories(currentLimit);

        if (itemList != null) {
            for (ItemResponse item : itemList) {
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
