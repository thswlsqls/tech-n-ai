package com.tech.n.ai.batch.source.domain.news.devto.reader;

import com.tech.n.ai.batch.source.domain.news.devto.service.NewsDevToApiService;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.Article;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class NewsDevToApiPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected NewsDevToApiService service;
    protected String tag;
    protected String username;
    protected String state;
    protected Integer top;
    protected Integer collectionId;
    protected Integer page;
    protected Integer perPage;

    public NewsDevToApiPagingItemReader(int pageSize
                                        , NewsDevToApiService service
                                        , String tag
                                        , String username
                                        , String state
                                        , Integer top
                                        , Integer collectionId
                                        , Integer page
                                        , Integer perPage) {
        setPageSize(pageSize);
        this.service = service;
        this.tag = tag;
        this.username = username;
        this.state = state;
        this.top = top;
        this.collectionId = collectionId;
        this.page = page;
        this.perPage = perPage;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // JobParameter에서 받은 값을 우선 사용, 없으면 기본값 사용
        String currentTag = (this.tag != null && !this.tag.isBlank()) ? this.tag : "technology";
        int currentPage = (this.page != null) ? this.page : (getPage() >= 0 ? getPage() : 1);
        int currentPerPage = (this.perPage != null) ? this.perPage : getPageSize();
        
        log.info("doReadPage ... tag: {}, page: {}, perPage: {}", currentTag, currentPage, currentPerPage);

        List<Article> itemList = service.getArticles(
            currentTag, username, state, top, collectionId, currentPage, currentPerPage);

        if (itemList != null) {
            for (Article item : itemList) {
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
