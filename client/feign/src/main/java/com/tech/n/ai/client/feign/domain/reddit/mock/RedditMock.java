package com.tech.n.ai.client.feign.domain.reddit.mock;

import com.tech.n.ai.client.feign.domain.reddit.contract.RedditContract;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.SubredditRequest;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.SubredditResponse;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.SubredditData;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class RedditMock implements RedditContract {

    @Override
    public SubredditResponse getSubredditHot(SubredditRequest request) {
        log.info("getSubredditHot: request={}", request);
        return SubredditResponse.builder()
                .kind("Listing")
                .data(SubredditData.builder()
                        .after(null)
                        .before(null)
                        .dist(0)
                        .modhash(null)
                        .geoFilter(null)
                        .children(Collections.emptyList())
                        .build())
                .build();
    }

}
