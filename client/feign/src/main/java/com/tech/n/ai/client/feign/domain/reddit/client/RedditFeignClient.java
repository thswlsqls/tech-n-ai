package com.tech.n.ai.client.feign.domain.reddit.client;

import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "RedditFeign", url = "${feign-clients.reddit.uri}")
public interface RedditFeignClient {

    @GetMapping(value = "/r/{subreddit}/hot.json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    RedditDto.SubredditResponse getSubredditHot(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("subreddit") String subreddit,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "after", required = false) String after,
            @RequestParam(value = "before", required = false) String before
    );

}
