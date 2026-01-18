package com.tech.n.ai.batch.source.domain.news.newsapi.reader;

import com.tech.n.ai.batch.source.domain.news.newsapi.service.NewsApiService;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto.Article;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class NewsApiPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected NewsApiService service;
    protected String query;
    protected String sources;
    protected String domains;
    protected String excludeDomains;
    protected String from;
    protected String to;
    protected String language;
    protected String sortBy;
    protected Integer pageSize;
    protected Integer page;

    public NewsApiPagingItemReader(int pageSize
                                  , NewsApiService service
                                  , String query
                                  , String sources
                                  , String domains
                                  , String excludeDomains
                                  , String from
                                  , String to
                                  , String language
                                  , String sortBy
                                  , Integer pageSizeParam
                                  , Integer page) {
        setPageSize(pageSize);
        this.service = service;
        this.query = query;
        this.sources = sources;
        this.domains = domains;
        this.excludeDomains = excludeDomains;
        this.from = from;
        this.to = to;
        this.language = language;
        this.sortBy = sortBy;
        this.pageSize = pageSizeParam;
        this.page = page;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // JobParameter에서 받은 값을 우선 사용, 없으면 기본값 사용
        String currentQuery = (this.query != null && !this.query.isBlank()) ? this.query : "technology";
        int currentPageSize = (this.pageSize != null) ? this.pageSize : getPageSize();
        int currentPage = (this.page != null) ? this.page : (getPage() >= 0 ? getPage() : 1);
        
        log.info("doReadPage ... query: {}, pageSize: {}, page: {}", currentQuery, currentPageSize, currentPage);

        List<Article> itemList = service.getEverything(
            currentQuery, sources, domains, excludeDomains, from, to, language, sortBy, currentPageSize, currentPage);

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
