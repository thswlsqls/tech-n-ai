package com.tech.n.ai.batch.source.domain.contest.codeforces.reader;


import com.tech.n.ai.batch.source.domain.contest.codeforces.service.CodeforcesApiService;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.Contest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;


@Slf4j
public class CodeforcesApiPagingItemReader <T> extends AbstractPagingItemReader<T> {

    protected CodeforcesApiService service;
    protected boolean gym;

    public CodeforcesApiPagingItemReader(int pageSize
                                       , CodeforcesApiService service
                                       , boolean gym
    ) {
        setPageSize(pageSize);
        this.service = service;
        this.gym = gym;
    }


    @Override
    protected void doReadPage() {
        initResults();

        List<Contest> itemList = service.getContestList(gym);

        int totalSize = itemList.size();
        int page = getPage() >= 0 ? getPage() : 1;
        int pageSize = getPageSize();
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalSize);

        log.info("doReadPage ... fromIndex : {}, toIndex : {}", fromIndex, toIndex);

        for (Contest item : itemList) {
            results.add((T) item);
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
