package com.tech.n.ai.client.feign.domain.codeforces.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class CodeforcesDto {

    @Builder
    public record ContestListRequest(
            @JsonProperty("gym")
            Boolean gym
    ) {
        public ContestListRequest() {
            this(false);
        }
    }

    @Builder
    public record ContestListResponse(
            @JsonProperty("status")
            String status,
            @JsonProperty("result")
            List<Contest> result
    ) {
    }

    @Builder
    public record Contest(
            @JsonProperty("id")
            Integer id,
            @JsonProperty("name")
            String name,
            @JsonProperty("type")
            String type,
            @JsonProperty("phase")
            String phase,
            @JsonProperty("frozen")
            Boolean frozen,
            @JsonProperty("durationSeconds")
            Integer durationSeconds,
            @JsonProperty("startTimeSeconds")
            Long startTimeSeconds,
            @JsonProperty("relativeTimeSeconds")
            Long relativeTimeSeconds,
            @JsonProperty("preparedBy")
            String preparedBy,
            @JsonProperty("websiteUrl")
            String websiteUrl,
            @JsonProperty("description")
            String description,
            @JsonProperty("difficulty")
            Integer difficulty,
            @JsonProperty("kind")
            String kind,
            @JsonProperty("icpcRegion")
            String icpcRegion,
            @JsonProperty("country")
            String country,
            @JsonProperty("city")
            String city,
            @JsonProperty("season")
            String season
    ) {
    }

}
