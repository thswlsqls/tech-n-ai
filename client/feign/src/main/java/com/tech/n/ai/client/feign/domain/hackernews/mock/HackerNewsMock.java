package com.tech.n.ai.client.feign.domain.hackernews.mock;

import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsContract;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemRequest;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemResponse;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.TopStoriesRequest;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.TopStoriesResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class HackerNewsMock implements HackerNewsContract {

    @Override
    public TopStoriesResponse getTopStories(TopStoriesRequest request) {
        log.info("getTopStories: request={}", request);
        return TopStoriesResponse.builder()
                .storyIds(Collections.emptyList())
                .build();
    }

    @Override
    public ItemResponse getItem(ItemRequest request) {
        log.info("getItem: request={}", request);
        return ItemResponse.builder()
                .id(request.id())
                .type("story")
                .build();
    }

}
