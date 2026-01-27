package com.tech.n.ai.datasource.mongodb.document;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 업데이트 정보 Document (MongoDB)
 */
@Document(collection = "ai_updates")
@Getter
@Setter
public class AiUpdateDocument {

    @Id
    private ObjectId id;

    @Field("provider")
    @Indexed
    private String provider;  // AiProvider enum value

    @Field("update_type")
    private String updateType;  // AiUpdateType enum value

    @Field("title")
    private String title;

    @Field("summary")
    private String summary;

    @Field("url")
    @Indexed
    private String url;

    @Field("published_at")
    @Indexed
    private LocalDateTime publishedAt;

    @Field("source_type")
    private String sourceType;  // SourceType enum value

    @Field("status")
    @Indexed
    private String status;  // PostStatus enum value

    @Field("metadata")
    private AiUpdateMetadata metadata;

    @Field("external_id")
    @Indexed(unique = true)
    private String externalId;  // 중복 체크용 (GitHub release ID 등)

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_by")
    private String updatedBy;

    /**
     * AI 업데이트 메타데이터
     */
    @Getter
    @Setter
    public static class AiUpdateMetadata {

        @Field("version")
        private String version;  // SDK 버전

        @Field("tags")
        private List<String> tags;

        @Field("author")
        private String author;

        @Field("github_repo")
        private String githubRepo;

        @Field("additional_info")
        private Map<String, Object> additionalInfo;
    }
}
