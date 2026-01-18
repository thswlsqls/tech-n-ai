package com.tech.n.ai.batch.source.domain.news.reddit.service;

import com.tech.n.ai.client.feign.domain.reddit.contract.RedditContract;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsRedditApiService {

    private final RedditContract redditApi;

    public List<RedditDto.Post> getSubredditPosts(String subreddit, Integer limit, String after, String before) {
        RedditDto.SubredditResponse response = redditApi.getSubredditHot(
            RedditDto.SubredditRequest.builder()
                .subreddit(subreddit)
                .limit(limit)
                .after(after)
                .before(before)
                .build());
        
        if (response.data() != null && response.data().children() != null) {
            return response.data().children().stream()
                .map(RedditDto.PostWrapper::data)
                .collect(Collectors.toList());
        }
        
        return List.of();
    }
}
