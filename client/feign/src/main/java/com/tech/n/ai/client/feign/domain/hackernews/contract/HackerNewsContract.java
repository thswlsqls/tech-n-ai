package com.tech.n.ai.client.feign.domain.hackernews.contract;

public interface HackerNewsContract {

    HackerNewsDto.TopStoriesResponse getTopStories(HackerNewsDto.TopStoriesRequest request);

    HackerNewsDto.ItemResponse getItem(HackerNewsDto.ItemRequest request);

}
