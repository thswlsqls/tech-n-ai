package com.tech.n.ai.client.feign.domain.github.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

public class GitHubDto {

    @Builder
    public record EventsRequest(
            @JsonProperty("per_page")
            Integer perPage,
            @JsonProperty("page")
            Integer page
    ) {
        public EventsRequest() {
            this(30, 1);
        }
    }

    @Builder
    public record EventsResponse(
            @JsonProperty("events")
            List<Event> events
    ) {
    }

    @Builder
    public record Event(
            @JsonProperty("id")
            String id,
            @JsonProperty("type")
            String type,
            @JsonProperty("actor")
            Actor actor,
            @JsonProperty("repo")
            Repository repo,
            @JsonProperty("payload")
            Map<String, Object> payload,
            @JsonProperty("public")
            Boolean isPublic,
            @JsonProperty("created_at")
            String createdAt,
            @JsonProperty("org")
            Organization org
    ) {
    }

    @Builder
    public record Actor(
            @JsonProperty("id")
            Long id,
            @JsonProperty("login")
            String login,
            @JsonProperty("display_login")
            String displayLogin,
            @JsonProperty("gravatar_id")
            String gravatarId,
            @JsonProperty("url")
            String url,
            @JsonProperty("avatar_url")
            String avatarUrl
    ) {
    }

    @Builder
    public record Repository(
            @JsonProperty("id")
            Long id,
            @JsonProperty("name")
            String name,
            @JsonProperty("url")
            String url
    ) {
    }

    @Builder
    public record Organization(
            @JsonProperty("id")
            Long id,
            @JsonProperty("login")
            String login,
            @JsonProperty("gravatar_id")
            String gravatarId,
            @JsonProperty("url")
            String url,
            @JsonProperty("avatar_url")
            String avatarUrl
    ) {
    }

}
