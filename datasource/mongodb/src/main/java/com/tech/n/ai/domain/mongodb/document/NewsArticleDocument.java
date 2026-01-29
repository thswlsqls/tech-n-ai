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

/**
 * NewsArticleDocument
 */
@Document(collection = "news_articles")
@Getter
@Setter
public class NewsArticleDocument {

    @Id
    private ObjectId id;

    @Field("source_id")
    @Indexed
    private ObjectId sourceId;

    @Field("title")
    private String title;

    @Field("content")
    private String content;

    @Field("summary")
    private String summary;

    @Field("published_at")
    private LocalDateTime publishedAt;

    @Field("url")
    private String url;

    @Field("author")
    private String author;

    @Field("metadata")
    private NewsArticleMetadata metadata;

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
     * NewsArticleMetadata
     */
    @Getter
    @Setter
    public static class NewsArticleMetadata {
        @Field("source_name")
        private String sourceName;

        @Field("tags")
        private List<String> tags;

        @Field("view_count")
        private Integer viewCount;

        @Field("like_count")
        private Integer likeCount;
    }
}
