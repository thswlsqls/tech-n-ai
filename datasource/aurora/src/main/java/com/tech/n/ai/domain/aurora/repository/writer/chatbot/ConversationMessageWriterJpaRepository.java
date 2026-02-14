package com.tech.n.ai.domain.mariadb.repository.writer.chatbot;

import com.tech.n.ai.domain.mariadb.entity.chatbot.ConversationMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ConversationMessageWriterJpaRepository
 */
@Repository
public interface ConversationMessageWriterJpaRepository extends JpaRepository<ConversationMessageEntity, Long> {
    
    List<ConversationMessageEntity> findBySessionIdOrderBySequenceNumberAsc(Long sessionId);
}
