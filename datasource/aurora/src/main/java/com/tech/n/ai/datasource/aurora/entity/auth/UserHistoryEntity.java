package com.tech.n.ai.datasource.aurora.entity.auth;

import com.tech.n.ai.datasource.aurora.annotation.Tsid;
import com.tech.n.ai.datasource.aurora.generator.TsidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

/**
 * UserHistoryEntity
 */
@Entity
@Table(name = "user_history")
@Getter
@Setter
public class UserHistoryEntity {

    @Id
    @Tsid
    @GeneratedValue(generator = "tsid-generator")
    @GenericGenerator(name = "tsid-generator", type = com.tech.n.ai.datasource.aurora.generator.TsidGenerator.class)
    @Column(name = "history_id", nullable = false, updatable = false)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false, nullable = false)
    private Long userId;

    @Column(name = "operation_type", length = 20, nullable = false)
    private String operationType;

    @Column(name = "before_data", columnDefinition = "JSON")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "JSON")
    private String afterData;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false, precision = 6)
    private LocalDateTime changedAt;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
