package com.tech.n.ai.datasource.aurora.entity.archive;

import com.tech.n.ai.datasource.aurora.entity.BaseEntity;
import com.tech.n.ai.datasource.aurora.listener.HistoryEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ArchiveEntity
 */
@Entity
@Table(name = "archives")
@EntityListeners(HistoryEntityListener.class)
@Getter
@Setter
public class ArchiveEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_type", length = 50, nullable = false)
    private String itemType;

    @Column(name = "item_id", length = 255, nullable = false)
    private String itemId;

    @Column(name = "tag", length = 100)
    private String tag;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;
}
