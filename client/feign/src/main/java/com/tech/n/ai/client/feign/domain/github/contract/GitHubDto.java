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

    // GitHub Releases API 관련 DTO
    @Builder
    public record ReleasesRequest(
            String owner,
            String repo,
            @JsonProperty("per_page")
            Integer perPage,
            @JsonProperty("page")
            Integer page
    ) {
        public ReleasesRequest(String owner, String repo) {
            this(owner, repo, 30, 1);
        }
    }

    @Builder
    public record ReleasesResponse(
            List<Release> releases
    ) {}

    @Builder
    public record Release(
            @JsonProperty("id")
            Long id,
            @JsonProperty("tag_name")
            String tagName,
            @JsonProperty("name")
            String name,
            @JsonProperty("body")
            String body,
            @JsonProperty("html_url")
            String htmlUrl,
            @JsonProperty("published_at")
            String publishedAt,
            @JsonProperty("prerelease")
            Boolean prerelease,
            @JsonProperty("draft")
            Boolean draft,
            @JsonProperty("author")
            Author author
    ) {}

    @Builder
    public record Author(
            @JsonProperty("login")
            String login,
            @JsonProperty("id")
            Long id,
            @JsonProperty("avatar_url")
            String avatarUrl
    ) {}

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
