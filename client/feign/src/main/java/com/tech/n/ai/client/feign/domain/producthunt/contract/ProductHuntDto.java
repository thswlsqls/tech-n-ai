package com.tech.n.ai.client.feign.domain.producthunt.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;

public class ProductHuntDto {

    @Builder
    public record GraphQLRequest(
            @JsonProperty("query")
            String query,
            @JsonProperty("variables")
            Map<String, Object> variables,
            @JsonProperty("operationName")
            String operationName
    ) {
    }

    @Builder
    public record GraphQLResponse(
            @JsonProperty("data")
            Map<String, Object> data,
            @JsonProperty("errors")
            java.util.List<GraphQLError> errors
    ) {
    }

    @Builder
    public record GraphQLError(
            @JsonProperty("message")
            String message,
            @JsonProperty("locations")
            java.util.List<ErrorLocation> locations,
            @JsonProperty("path")
            java.util.List<Object> path,
            @JsonProperty("extensions")
            Map<String, Object> extensions
    ) {
    }

    @Builder
    public record ErrorLocation(
            @JsonProperty("line")
            Integer line,
            @JsonProperty("column")
            Integer column
    ) {
    }

}
