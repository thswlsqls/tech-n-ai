package com.tech.n.ai.domain.mongodb.repository;

import com.tech.n.ai.domain.mongodb.document.NewsArticleDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NewsArticleRepository
 */
@Repository
public interface NewsArticleRepository extends MongoRepository<NewsArticleDocument, ObjectId> {
    List<NewsArticleDocument> findBySourceIdOrderByPublishedAtDesc(ObjectId sourceId);
    List<NewsArticleDocument> findByPublishedAtGreaterThanEqualOrderByPublishedAtDesc(LocalDateTime publishedAt);
    boolean existsBySourceIdAndUrl(ObjectId sourceId, String url);
}
