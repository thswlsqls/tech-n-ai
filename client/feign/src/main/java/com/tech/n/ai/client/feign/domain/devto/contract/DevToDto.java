package com.tech.n.ai.client.feign.domain.devto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class DevToDto {

    @Builder
    public record ArticlesRequest(
            @JsonProperty("tag")
            String tag,
            @JsonProperty("username")
            String username,
            @JsonProperty("state")
            String state,
            @JsonProperty("top")
            Integer top,
            @JsonProperty("collection_id")
            Integer collectionId,
            @JsonProperty("page")
            Integer page,
            @JsonProperty("per_page")
            Integer perPage
    ) {
        public ArticlesRequest() {
            this("technology", null, null, null, null, 1, 30);
        }
    }

    @Builder
    public record ArticlesResponse(
            @JsonProperty("articles")
            List<Article> articles
    ) {
    }

    @Builder
    public record Article(
            @JsonProperty("id")
            Integer id,
            @JsonProperty("title")
            String title,
            @JsonProperty("description")
            String description,
            @JsonProperty("cover_image")
            String coverImage,
            @JsonProperty("readable_publish_date")
            String readablePublishDate,
            @JsonProperty("social_image")
            String socialImage,
            @JsonProperty("tag_list")
            List<String> tagList,
            @JsonProperty("tags")
            String tags,
            @JsonProperty("slug")
            String slug,
            @JsonProperty("path")
            String path,
            @JsonProperty("url")
            String url,
            @JsonProperty("canonical_url")
            String canonicalUrl,
            @JsonProperty("comments_count")
            Integer commentsCount,
            @JsonProperty("positive_reactions_count")
            Integer positiveReactionsCount,
            @JsonProperty("public_reactions_count")
            Integer publicReactionsCount,
            @JsonProperty("created_at")
            String createdAt,
            @JsonProperty("edited_at")
            String editedAt,
            @JsonProperty("crossposted_at")
            String crosspostedAt,
            @JsonProperty("published_at")
            String publishedAt,
            @JsonProperty("last_comment_at")
            String lastCommentAt,
            @JsonProperty("published_timestamp")
            String publishedTimestamp,
            @JsonProperty("reading_time_minutes")
            Integer readingTimeMinutes,
            @JsonProperty("user")
            User user,
            @JsonProperty("organization")
            Organization organization,
            @JsonProperty("flare_tag")
            FlareTag flareTag
    ) {
    }

    @Builder
    public record User(
            @JsonProperty("name")
            String name,
            @JsonProperty("username")
            String username,
            @JsonProperty("twitter_username")
            String twitterUsername,
            @JsonProperty("github_username")
            String githubUsername,
            @JsonProperty("website_url")
            String websiteUrl,
            @JsonProperty("profile_image")
            String profileImage,
            @JsonProperty("profile_image_90")
            String profileImage90
    ) {
    }

    @Builder
    public record Organization(
            @JsonProperty("name")
            String name,
            @JsonProperty("username")
            String username,
            @JsonProperty("slug")
            String slug,
            @JsonProperty("profile_image")
            String profileImage,
            @JsonProperty("profile_image_90")
            String profileImage90
    ) {
    }

    @Builder
    public record FlareTag(
            @JsonProperty("name")
            String name,
            @JsonProperty("bg_color_hex")
            String bgColorHex,
            @JsonProperty("text_color_hex")
            String textColorHex
    ) {
    }

}
