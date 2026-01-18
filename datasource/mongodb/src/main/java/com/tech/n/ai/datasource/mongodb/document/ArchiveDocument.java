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
 * ArchiveDocument
 */
@Document(collection = "archives")
@Getter
@Setter
public class ArchiveDocument {

    @Id
    private ObjectId id;

    @Field("archive_tsid")
    @Indexed(unique = true)
    private String archiveTsid;

    @Field("user_id")
    private String userId;

    @Field("item_type")
    private String itemType;

    @Field("item_id")
    private ObjectId itemId;

    @Field("item_title")
    private String itemTitle;

    @Field("item_summary")
    private String itemSummary;

    @Field("tag")
    private String tag;

    @Field("memo")
    private String memo;

    @Field("archived_at")
    private LocalDateTime archivedAt;

    @Field("item_start_date")
    private LocalDateTime itemStartDate;  // Contest의 startDate

    @Field("item_end_date")
    private LocalDateTime itemEndDate;    // Contest의 endDate

    @Field("item_published_at")
    private LocalDateTime itemPublishedAt; // News의 publishedAt

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
}
