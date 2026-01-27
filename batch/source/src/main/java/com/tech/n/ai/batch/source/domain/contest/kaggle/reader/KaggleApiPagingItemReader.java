package com.tech.n.ai.batch.source.domain.contest.kaggle.reader;

import com.tech.n.ai.batch.source.domain.contest.kaggle.service.KaggleApiService;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto.Competition;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class KaggleApiPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected KaggleApiService service;
    protected Integer page;
    protected String search;
    protected String category;
    protected String sortBy;
    protected String group;
    protected String filter;

    public KaggleApiPagingItemReader(int pageSize
                                   , KaggleApiService service
                                   , Integer page
                                   , String search
                                   , String category
                                   , String sortBy
                                   , String group
                                   , String filter) {
        setPageSize(pageSize);
        this.service = service;
        this.page = page;
        this.search = search;
        this.category = category;
        this.sortBy = sortBy;
        this.group = group;
        this.filter = filter;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // JobParameter에서 받은 값을 우선 사용, 없으면 기본값 사용
        int currentPage = (this.page != null) ? this.page : (getPage() >= 0 ? getPage() : 1);
        
        log.info("doReadPage ... page: {}, search: {}, category: {}", currentPage, search, category);

        List<Competition> itemList = service.getCompetitionsList(
            currentPage, search, category, sortBy, group, filter);

        if (itemList != null) {
            for (Competition item : itemList) {
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
