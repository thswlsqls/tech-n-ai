package com.tech.n.ai.domain.mongodb.document;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 대화 세션 Document
 */
@Document(collection = "conversation_sessions")
@Getter
@Setter
public class ConversationSessionDocument {
    
    @Id
    private ObjectId id;
    
    @Field("session_id")
    @Indexed(unique = true)
    private String sessionId;  // TSID (String)
    
    @Field("user_id")
    @Indexed
    private String userId;
    
    @Field("title")
    private String title;
    
    @Field("last_message_at")
    @Indexed
    private LocalDateTime lastMessageAt;
    
    @Field("is_active")
    @Indexed
    private Boolean isActive;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
