package com.tech.n.ai.batch.source.domain.news.reddit.reader;

import com.tech.n.ai.batch.source.domain.news.reddit.service.NewsRedditApiService;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.Post;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class NewsRedditApiPagingItemReader<T> extends AbstractPagingItemReader<T> {

    protected NewsRedditApiService service;
    protected String subreddit;
    protected Integer limit;
    protected String after;
    protected String before;

    public NewsRedditApiPagingItemReader(int pageSize
                                        , NewsRedditApiService service
                                        , String subreddit
                                        , Integer limit
                                        , String after
                                        , String before) {
        setPageSize(pageSize);
        this.service = service;
        this.subreddit = subreddit;
        this.limit = limit;
        this.after = after;
        this.before = before;
    }

    @Override
    protected void doReadPage() {
        initResults();

        // JobParameter에서 받은 값을 우선 사용, 없으면 기본값 사용
        String currentSubreddit = (this.subreddit != null && !this.subreddit.isBlank()) 
            ? this.subreddit 
            : "programming";
        int currentLimit = (this.limit != null) ? this.limit : getPageSize();
        
        log.info("doReadPage ... subreddit: {}, limit: {}, after: {}", currentSubreddit, currentLimit, after);

        List<Post> itemList = service.getSubredditPosts(currentSubreddit, currentLimit, after, before);

        if (itemList != null) {
            for (Post item : itemList) {
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
