package com.tech.n.ai.datasource.aurora.repository.writer.chatbot;

import com.tech.n.ai.datasource.aurora.entity.chatbot.ConversationSessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ConversationSessionWriterRepository
 */
@Service
@RequiredArgsConstructor
public class ConversationSessionWriterRepository {
    
    private final ConversationSessionWriterJpaRepository conversationSessionWriterJpaRepository;
    
    public ConversationSessionEntity save(ConversationSessionEntity entity) {
        return conversationSessionWriterJpaRepository.save(entity);
    }
    
    public ConversationSessionEntity saveAndFlush(ConversationSessionEntity entity) {
        return conversationSessionWriterJpaRepository.saveAndFlush(entity);
    }
    
    public void delete(ConversationSessionEntity entity) {
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        conversationSessionWriterJpaRepository.save(entity);
    }
}
