package com.tech.n.ai.batch.source.domain.news.hackernews.service;

import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsContract;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsHackerNewsApiService {

    private final HackerNewsContract hackerNewsApi;

    /**
     * Top stories의 ID 목록 조회
     */
    public List<Long> getTopStoryIds() {
        HackerNewsDto.TopStoriesResponse response = hackerNewsApi.getTopStories(
            HackerNewsDto.TopStoriesRequest.builder().build());
        return response.storyIds();
    }

    /**
     * Story ID로 Item 조회
     */
    public HackerNewsDto.ItemResponse getItem(Long id) {
        return hackerNewsApi.getItem(HackerNewsDto.ItemRequest.builder().id(id).build());
    }

    /**
     * Top stories의 Item 목록 조회
     */
    public List<HackerNewsDto.ItemResponse> getTopStories(Integer limit) {
        List<Long> storyIds = getTopStoryIds();
        
        if (storyIds == null || storyIds.isEmpty()) {
            return List.of();
        }
        
        // limit이 있으면 제한
        if (limit != null && limit > 0) {
            storyIds = storyIds.stream().limit(limit).collect(Collectors.toList());
        }
        
        // 각 story ID로 Item 조회
        return storyIds.stream()
            .map(this::getItem)
            .filter(item -> item != null && !Boolean.TRUE.equals(item.deleted()) && !Boolean.TRUE.equals(item.dead()))
            .filter(item -> "story".equals(item.type())) // story 타입만 필터링
            .collect(Collectors.toList());
    }
}
