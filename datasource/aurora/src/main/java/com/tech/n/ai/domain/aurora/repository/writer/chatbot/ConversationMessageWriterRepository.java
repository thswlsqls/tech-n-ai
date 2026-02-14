package com.tech.n.ai.domain.mariadb.repository.writer.chatbot;

import com.tech.n.ai.domain.mariadb.entity.chatbot.ConversationMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ConversationMessageWriterRepository
 */
@Service
@RequiredArgsConstructor
public class ConversationMessageWriterRepository {
    
    private final ConversationMessageWriterJpaRepository conversationMessageWriterJpaRepository;
    
    public ConversationMessageEntity save(ConversationMessageEntity entity) {
        return conversationMessageWriterJpaRepository.save(entity);
    }
    
    public ConversationMessageEntity saveAndFlush(ConversationMessageEntity entity) {
        return conversationMessageWriterJpaRepository.saveAndFlush(entity);
    }
}
