package com.tech.n.ai.datasource.mongodb.repository;

import com.tech.n.ai.datasource.mongodb.document.ExceptionLogDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ExceptionLogRepository
 */
@Repository
public interface ExceptionLogRepository extends MongoRepository<ExceptionLogDocument, ObjectId> {
    List<ExceptionLogDocument> findBySourceOrderByOccurredAtDesc(String source);
    List<ExceptionLogDocument> findByExceptionTypeOrderByOccurredAtDesc(String exceptionType);
    List<ExceptionLogDocument> findByOccurredAtGreaterThanEqualOrderByOccurredAtDesc(LocalDateTime occurredAt);
}
