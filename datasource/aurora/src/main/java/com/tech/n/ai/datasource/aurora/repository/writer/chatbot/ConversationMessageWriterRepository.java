package com.tech.n.ai.datasource.aurora.repository.writer.chatbot;

import com.tech.n.ai.datasource.aurora.entity.chatbot.ConversationMessageEntity;
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
