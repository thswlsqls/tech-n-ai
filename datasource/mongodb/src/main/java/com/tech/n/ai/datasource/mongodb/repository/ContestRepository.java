package com.tech.n.ai.datasource.mongodb.repository;

import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ContestRepository
 */
@Repository
public interface ContestRepository extends MongoRepository<ContestDocument, ObjectId> {
    List<ContestDocument> findBySourceIdOrderByStartDateDesc(ObjectId sourceId);
    List<ContestDocument> findByStatusOrderByStartDateDesc(String status);
    boolean existsBySourceIdAndUrl(ObjectId sourceId, String url);
}
