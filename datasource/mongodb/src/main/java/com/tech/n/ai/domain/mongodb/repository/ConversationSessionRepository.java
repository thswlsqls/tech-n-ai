package com.tech.n.ai.domain.mongodb.repository;

import com.tech.n.ai.domain.mongodb.document.ConversationSessionDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ConversationSessionRepository
 */
@Repository
public interface ConversationSessionRepository extends MongoRepository<ConversationSessionDocument, ObjectId> {
    
    Optional<ConversationSessionDocument> findBySessionId(String sessionId);
    
    List<ConversationSessionDocument> findByUserIdOrderByLastMessageAtDesc(String userId);
    
    List<ConversationSessionDocument> findByUserIdAndIsActiveTrueOrderByLastMessageAtDesc(String userId);
}
