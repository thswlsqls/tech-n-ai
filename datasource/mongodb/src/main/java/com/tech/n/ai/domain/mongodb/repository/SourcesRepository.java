package com.tech.n.ai.domain.mongodb.repository;

import com.tech.n.ai.domain.mongodb.document.SourcesDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourcesRepository extends MongoRepository<SourcesDocument, org.bson.types.ObjectId> {
    
    Optional<SourcesDocument> findByUrlAndCategory(String url, String category);
}
