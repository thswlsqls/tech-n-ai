package com.tech.n.ai.client.feign.domain.reddit.api;

import com.tech.n.ai.client.feign.domain.reddit.client.RedditFeignClient;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditContract;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.SubredditRequest;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.SubredditResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RequiredArgsConstructor
public class RedditApi implements RedditContract {

    private final RedditFeignClient redditFeign;

    @Value("${feign-clients.reddit.token:}")
    private String token;

    @Override
    public SubredditResponse getSubredditHot(SubredditRequest request) {
        String authorization = token != null && !token.isEmpty() ? "Bearer " + token : null;
        return redditFeign.getSubredditHot(
                authorization,
                request.subreddit(),
                request.limit(),
                request.after(),
                request.before()
        );
    }

}
