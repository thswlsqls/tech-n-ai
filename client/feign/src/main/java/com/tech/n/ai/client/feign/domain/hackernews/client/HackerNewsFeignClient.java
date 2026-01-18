package com.tech.n.ai.client.feign.domain.hackernews.client;

import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "HackerNewsFeign", url = "${feign-clients.hackernews.uri}")
public interface HackerNewsFeignClient {

    @GetMapping(value = "/topstories.json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<Long> getTopStories();

    @GetMapping(value = "/item/{id}.json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    HackerNewsDto.ItemResponse getItem(@PathVariable("id") Long id);

}
