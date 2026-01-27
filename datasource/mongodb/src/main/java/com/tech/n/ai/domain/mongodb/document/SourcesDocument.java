package com.tech.n.ai.datasource.mongodb.document;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * SourcesDocument
 */
@Document(collection = "sources")
@Getter
@Setter
public class SourcesDocument {

    @Id
    private ObjectId id;

    @Field("name")
    @Indexed(unique = true)
    private String name;

    @Field("type")
    private String type;

    @Field("category")
    private String category;

    @Field("url")
    private String url;

    @Field("api_endpoint")
    private String apiEndpoint;

    @Field("rss_feed_url")
    private String rssFeedUrl;

    @Field("description")
    private String description;

    @Field("reliability_score")
    private Integer reliabilityScore;

    @Field("accessibility_score")
    private Integer accessibilityScore;

    @Field("data_quality_score")
    private Integer dataQualityScore;

    @Field("legal_ethical_score")
    private Integer legalEthicalScore;

    @Field("total_score")
    private Integer totalScore;

    @Field("priority")
    private Integer priority;

    @Field("authentication_required")
    private Boolean authenticationRequired;

    @Field("authentication_method")
    private String authenticationMethod;

    @Field("rate_limit")
    private String rateLimit;

    @Field("documentation_url")
    private String documentationUrl;

    @Field("update_frequency")
    private String updateFrequency;

    @Field("data_format")
    private String dataFormat;

    @Field("pros")
    private List<String> pros;

    @Field("cons")
    private List<String> cons;

    @Field("implementation_difficulty")
    private String implementationDifficulty;

    @Field("cost")
    private String cost;

    @Field("cost_details")
    private String costDetails;

    @Field("recommended_use_case")
    private String recommendedUseCase;

    @Field("integration_example")
    private String integrationExample;

    @Field("alternative_sources")
    private List<String> alternativeSources;

    @Field("enabled")
    private Boolean enabled;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("updated_by")
    private String updatedBy;
}
