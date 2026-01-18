package com.tech.n.ai.datasource.aurora.entity.chatbot;

import com.tech.n.ai.datasource.aurora.annotation.Tsid;
import com.tech.n.ai.datasource.aurora.entity.BaseEntity;
import com.tech.n.ai.datasource.aurora.generator.TsidGenerator;
import com.tech.n.ai.datasource.aurora.listener.HistoryEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

/**
 * 대화 세션 엔티티
 */
@Entity
@Table(name = "conversation_sessions", schema = "chatbot")
@EntityListeners(HistoryEntityListener.class)
@Getter
@Setter
public class ConversationSessionEntity extends BaseEntity {
    
    @Id
    @Tsid
    @GeneratedValue(generator = "tsid-generator")
    @GenericGenerator(name = "tsid-generator", type = TsidGenerator.class)
    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;  // TSID Primary Key (BaseEntity의 id 대신 사용)
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "title", length = 200)
    private String title;  // 선택: 세션 제목
    
    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;  // 활성 세션 여부
    
    // BaseEntity에서 상속: id (TSID), isDeleted, deletedAt, deletedBy, createdAt, createdBy, updatedAt, updatedBy
}
