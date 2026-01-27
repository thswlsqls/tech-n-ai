package com.tech.n.ai.datasource.mongodb.document;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 대화 메시지 Document
 */
@Document(collection = "conversation_messages")
@Getter
@Setter
public class ConversationMessageDocument {
    
    @Id
    private ObjectId id;
    
    @Field("message_id")
    @Indexed(unique = true)
    private String messageId;  // TSID (String)
    
    @Field("session_id")
    @Indexed
    private String sessionId;
    
    @Field("role")
    @Indexed
    private String role;  // USER, ASSISTANT, SYSTEM
    
    @Field("content")
    private String content;
    
    @Field("token_count")
    private Integer tokenCount;
    
    @Field("sequence_number")
    @Indexed
    private Integer sequenceNumber;
    
    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;
}
