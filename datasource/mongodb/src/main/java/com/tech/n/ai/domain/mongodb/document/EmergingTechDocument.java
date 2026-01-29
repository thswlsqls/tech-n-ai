package com.tech.n.ai.domain.mongodb.document;

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
 * Emerging Tech 업데이트 정보 Document (MongoDB)
 */
@Document(collection = "emerging_techs")
@Getter
@Setter
public class EmergingTechDocument {

    @Id
    private ObjectId id;

    @Field("provider")
    @Indexed
    private String provider;  // TechProvider enum value

    @Field("update_type")
    private String updateType;  // EmergingTechType enum value

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
    private EmergingTechMetadata metadata;

    @Field("external_id")
    @Indexed(unique = true)
    private String externalId;  // 중복 체크용 (GitHub release ID 등)

    @Field("embedding_text")
    private String embeddingText;  // 임베딩 대상 텍스트

    @Field("embedding_vector")
    private List<Float> embeddingVector;  // 벡터 필드 (1536차원 - OpenAI text-embedding-3-small 기본값)

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_by")
    private String updatedBy;

    /**
     * Emerging Tech 메타데이터
     */
    @Getter
    @Setter
    public static class EmergingTechMetadata {

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
