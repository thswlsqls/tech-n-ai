package com.tech.n.ai.client.feign.domain.newsapi.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class NewsAPIDto {

    @Builder
    public record EverythingRequest(
            @JsonProperty("q")
            String query,
            @JsonProperty("sources")
            String sources,
            @JsonProperty("domains")
            String domains,
            @JsonProperty("excludeDomains")
            String excludeDomains,
            @JsonProperty("from")
            String from,
            @JsonProperty("to")
            String to,
            @JsonProperty("language")
            String language,
            @JsonProperty("sortBy")
            String sortBy,
            @JsonProperty("pageSize")
            Integer pageSize,
            @JsonProperty("page")
            Integer page
    ) {
        public EverythingRequest() {
            this("technology", null, null, null, null, null, null, null, null, null);
        }
    }

    @Builder
    public record EverythingResponse(
            @JsonProperty("status")
            String status,
            @JsonProperty("totalResults")
            Integer totalResults,
            @JsonProperty("articles")
            List<Article> articles
    ) {
    }

    @Builder
    public record Article(
            @JsonProperty("source")
            Source source,
            @JsonProperty("author")
            String author,
            @JsonProperty("title")
            String title,
            @JsonProperty("description")
            String description,
            @JsonProperty("url")
            String url,
            @JsonProperty("urlToImage")
            String urlToImage,
            @JsonProperty("publishedAt")
            String publishedAt,
            @JsonProperty("content")
            String content
    ) {
    }

    @Builder
    public record Source(
            @JsonProperty("id")
            String id,
            @JsonProperty("name")
            String name
    ) {
    }

}
