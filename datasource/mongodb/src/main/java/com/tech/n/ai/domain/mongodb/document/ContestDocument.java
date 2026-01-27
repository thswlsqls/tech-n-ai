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

/**
 * ContestDocument
 */
@Document(collection = "contests")
@Getter
@Setter
public class ContestDocument {

    @Id
    private ObjectId id;

    @Field("source_id")
    @Indexed
    private ObjectId sourceId;

    @Field("title")
    private String title;

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

    @Field("status")
    private String status;

    @Field("description")
    private String description;

    @Field("url")
    private String url;

    @Field("metadata")
    private ContestMetadata metadata;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("updated_by")
    private String updatedBy;

    @Field("embedding_text")
    private String embeddingText;  // 임베딩 대상 텍스트

    @Field("embedding_vector")
    private List<Float> embeddingVector;  // 벡터 필드 (1536차원 - OpenAI text-embedding-3-small 기본값)

    /**
     * ContestMetadata
     */
    @Getter
    @Setter
    public static class ContestMetadata {
        @Field("source_name")
        private String sourceName;

        @Field("prize")
        private String prize;

        @Field("participants")
        private Integer participants;

        @Field("tags")
        private List<String> tags;
    }
}
