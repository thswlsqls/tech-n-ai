package com.tech.n.ai.client.feign.domain.kaggle.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class KaggleDto {

    @Builder
    public record CompetitionsListRequest(
            @JsonProperty("page")
            Integer page,
            @JsonProperty("search")
            String search,
            @JsonProperty("category")
            String category,
            @JsonProperty("sortBy")
            String sortBy,
            @JsonProperty("group")
            String group,
            @JsonProperty("filter")
            String filter
    ) {
        public CompetitionsListRequest() {
            this(null, null, null, null, null, null);
        }
    }

    @Builder
    public record CompetitionsListResponse(
            @JsonProperty("ref")
            String ref,
            @JsonProperty("totalResults")
            Integer totalResults,
            @JsonProperty("page")
            Integer page,
            @JsonProperty("pageSize")
            Integer pageSize,
            @JsonProperty("results")
            List<Competition> results
    ) {
    }

    @Builder
    public record Competition(
            @JsonProperty("id")
            Integer id,
            @JsonProperty("ref")
            String ref,
            @JsonProperty("title")
            String title,
            @JsonProperty("description")
            String description,
            @JsonProperty("category")
            String category,
            @JsonProperty("reward")
            String reward,
            @JsonProperty("teamCount")
            Integer teamCount,
            @JsonProperty("organizationName")
            String organizationName,
            @JsonProperty("organizationRef")
            String organizationRef,
            @JsonProperty("deadline")
            String deadline,
            @JsonProperty("enabledDate")
            String enabledDate,
            @JsonProperty("mergerDeadline")
            String mergerDeadline,
            @JsonProperty("submissionDeadline")
            String submissionDeadline,
            @JsonProperty("awardPoints")
            Integer awardPoints,
            @JsonProperty("evaluationMetric")
            String evaluationMetric,
            @JsonProperty("maxDailySubmissions")
            Integer maxDailySubmissions,
            @JsonProperty("maxTeamSize")
            Integer maxTeamSize,
            @JsonProperty("tags")
            List<String> tags,
            @JsonProperty("hasKernels")
            Boolean hasKernels,
            @JsonProperty("hasLeaderboard")
            Boolean hasLeaderboard,
            @JsonProperty("isGated")
            Boolean isGated,
            @JsonProperty("isPrivate")
            Boolean isPrivate,
            @JsonProperty("url")
            String url
    ) {
    }

}
