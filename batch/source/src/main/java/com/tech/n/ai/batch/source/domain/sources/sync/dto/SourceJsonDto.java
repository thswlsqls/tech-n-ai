package com.tech.n.ai.batch.source.domain.sources.sync.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SourceJsonDto
 * json/sources.json 파싱용 DTO 클래스
 * Jackson의 SnakeCaseStrategy를 사용하여 JSON 필드명(snake_case)을 Java 필드명(camelCase)으로 자동 매핑
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SourceJsonDto {

    private String name;
    private String type;
    private String category;
    private String url;
    private String apiEndpoint;
    private String rssFeedUrl;
    private String description;
    private Integer reliabilityScore;
    private Integer accessibilityScore;
    private Integer dataQualityScore;
    private Integer legalEthicalScore;
    private Integer totalScore;
    private Integer priority;
    private Boolean authenticationRequired;
    private String authenticationMethod;
    private String rateLimit;
    private String documentationUrl;
    private String updateFrequency;
    private String dataFormat;
    private List<String> pros;
    private List<String> cons;
    private String implementationDifficulty;
    private String cost;
    private String costDetails;
    private String recommendedUseCase;
    private String integrationExample;
    private List<String> alternativeSources;
}
