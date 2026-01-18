package com.tech.n.ai.client.feign.domain.hackernews.api;

import com.tech.n.ai.client.feign.domain.hackernews.client.HackerNewsFeignClient;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsContract;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemRequest;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemResponse;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.TopStoriesRequest;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.TopStoriesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class HackerNewsApi implements HackerNewsContract {

    private final HackerNewsFeignClient hackerNewsFeign;

    @Override
    public TopStoriesResponse getTopStories(TopStoriesRequest request) {
        List<Long> storyIds = hackerNewsFeign.getTopStories();
        return TopStoriesResponse.builder()
                .storyIds(storyIds != null ? storyIds : List.of())
                .build();
    }

    @Override
    public ItemResponse getItem(ItemRequest request) {
        return hackerNewsFeign.getItem(request.id());
    }

}
