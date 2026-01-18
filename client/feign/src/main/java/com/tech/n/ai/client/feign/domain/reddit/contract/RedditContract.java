package com.tech.n.ai.client.feign.domain.reddit.contract;

public interface RedditContract {

    RedditDto.SubredditResponse getSubredditHot(RedditDto.SubredditRequest request);

}
