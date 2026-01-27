package com.tech.n.ai.datasource.mariadb.entity.archive;

import com.tech.n.ai.datasource.mariadb.annotation.Tsid;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ArchiveHistoryEntity
 */
@Entity
@Table(name = "archive_history")
@Getter
@Setter
public class ArchiveHistoryEntity {

    @Id
    @Tsid
    @Column(name = "history_id", nullable = false, updatable = false)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", nullable = false)
    private ArchiveEntity archive;

    @Column(name = "archive_id", insertable = false, updatable = false, nullable = false)
    private Long archiveId;

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
