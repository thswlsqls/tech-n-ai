package com.tech.n.ai.client.feign.domain.hackernews.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class HackerNewsDto {

    @Builder
    public record TopStoriesRequest(
    ) {
        public TopStoriesRequest() {
        }
    }

    @Builder
    public record TopStoriesResponse(
            @JsonProperty("storyIds")
            List<Long> storyIds
    ) {
    }

    @Builder
    public record ItemRequest(
            @JsonProperty("id")
            Long id
    ) {
    }

    @Builder
    public record ItemResponse(
            @JsonProperty("id")
            Long id,
            @JsonProperty("deleted")
            Boolean deleted,
            @JsonProperty("type")
            String type,
            @JsonProperty("by")
            String by,
            @JsonProperty("time")
            Long time,
            @JsonProperty("text")
            String text,
            @JsonProperty("dead")
            Boolean dead,
            @JsonProperty("parent")
            Long parent,
            @JsonProperty("poll")
            Long poll,
            @JsonProperty("kids")
            List<Long> kids,
            @JsonProperty("url")
            String url,
            @JsonProperty("score")
            Integer score,
            @JsonProperty("title")
            String title,
            @JsonProperty("parts")
            List<Long> parts,
            @JsonProperty("descendants")
            Integer descendants
    ) {
    }

}
