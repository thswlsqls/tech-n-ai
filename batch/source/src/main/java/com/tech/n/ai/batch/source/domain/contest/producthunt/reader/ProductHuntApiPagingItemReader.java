package com.tech.n.ai.batch.source.domain.contest.producthunt.reader;

import com.tech.n.ai.batch.source.domain.contest.producthunt.service.ProductHuntApiService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class ProductHuntApiPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected ProductHuntApiService service;
    protected Integer first;
    protected String after;

    public ProductHuntApiPagingItemReader(int pageSize
                                         , ProductHuntApiService service
                                         , Integer first
                                         , String after) {
        setPageSize(pageSize);
        this.service = service;
        this.first = first;
        this.after = after;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // JobParameter에서 받은 값을 우선 사용, 없으면 기본값 사용
        int currentFirst = (this.first != null) ? this.first : getPageSize();
        String currentAfter = this.after; // 첫 페이지 이후에만 사용
        
        log.info("doReadPage ... first: {}, after: {}", currentFirst, currentAfter);

        List<Map<String, Object>> itemList = service.getPosts(currentFirst, currentAfter);

        if (itemList != null) {
            for (Map<String, Object> item : itemList) {
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
