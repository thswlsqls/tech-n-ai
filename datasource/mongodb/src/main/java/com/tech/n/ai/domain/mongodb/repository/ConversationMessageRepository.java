package com.tech.n.ai.datasource.mongodb.repository;

import com.tech.n.ai.datasource.mongodb.document.ConversationMessageDocument;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ConversationMessageRepository
 */
@Repository
public interface ConversationMessageRepository extends MongoRepository<ConversationMessageDocument, ObjectId> {
    
    Optional<ConversationMessageDocument> findByMessageId(String messageId);
    
    List<ConversationMessageDocument> findBySessionIdOrderBySequenceNumberAsc(String sessionId);
    
    Page<ConversationMessageDocument> findBySessionIdOrderBySequenceNumberAsc(String sessionId, Pageable pageable);
}
