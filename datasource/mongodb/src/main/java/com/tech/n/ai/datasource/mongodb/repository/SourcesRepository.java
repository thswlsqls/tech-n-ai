package com.tech.n.ai.datasource.mongodb.repository;

import com.tech.n.ai.datasource.mongodb.document.SourcesDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * SourcesRepository
 */
@Repository
public interface SourcesRepository extends MongoRepository<SourcesDocument, org.bson.types.ObjectId> {
}
