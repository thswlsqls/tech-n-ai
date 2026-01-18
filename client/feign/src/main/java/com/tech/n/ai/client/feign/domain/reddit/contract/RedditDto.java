package com.tech.n.ai.client.feign.domain.reddit.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class RedditDto {

    @Builder
    public record SubredditRequest(
            @JsonProperty("subreddit")
            String subreddit,
            @JsonProperty("limit")
            Integer limit,
            @JsonProperty("after")
            String after,
            @JsonProperty("before")
            String before
    ) {
        public SubredditRequest() {
            this("programming", 25, null, null);
        }
    }

    @Builder
    public record SubredditResponse(
            @JsonProperty("kind")
            String kind,
            @JsonProperty("data")
            SubredditData data
    ) {
    }

    @Builder
    public record SubredditData(
            @JsonProperty("after")
            String after,
            @JsonProperty("before")
            String before,
            @JsonProperty("dist")
            Integer dist,
            @JsonProperty("modhash")
            String modhash,
            @JsonProperty("geo_filter")
            String geoFilter,
            @JsonProperty("children")
            List<PostWrapper> children
    ) {
    }

    @Builder
    public record PostWrapper(
            @JsonProperty("kind")
            String kind,
            @JsonProperty("data")
            Post data
    ) {
    }

    @Builder
    public record Post(
            @JsonProperty("id")
            String id,
            @JsonProperty("name")
            String name,
            @JsonProperty("title")
            String title,
            @JsonProperty("selftext")
            String selftext,
            @JsonProperty("author")
            String author,
            @JsonProperty("created_utc")
            Long createdUtc,
            @JsonProperty("score")
            Integer score,
            @JsonProperty("ups")
            Integer ups,
            @JsonProperty("downs")
            Integer downs,
            @JsonProperty("num_comments")
            Integer numComments,
            @JsonProperty("url")
            String url,
            @JsonProperty("permalink")
            String permalink,
            @JsonProperty("subreddit")
            String subreddit,
            @JsonProperty("subreddit_id")
            String subredditId,
            @JsonProperty("domain")
            String domain,
            @JsonProperty("is_self")
            Boolean isSelf,
            @JsonProperty("over_18")
            Boolean over18,
            @JsonProperty("stickied")
            Boolean stickied,
            @JsonProperty("locked")
            Boolean locked,
            @JsonProperty("thumbnail")
            String thumbnail,
            @JsonProperty("thumbnail_width")
            Integer thumbnailWidth,
            @JsonProperty("thumbnail_height")
            Integer thumbnailHeight
    ) {
    }

}
