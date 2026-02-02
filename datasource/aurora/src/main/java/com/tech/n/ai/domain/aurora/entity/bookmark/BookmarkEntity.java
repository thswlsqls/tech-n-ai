package com.tech.n.ai.domain.aurora.entity.bookmark;

import com.tech.n.ai.domain.aurora.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * BookmarkEntity
 */
@Entity
@Table(name = "bookmarks")
@Getter
@Setter
public class BookmarkEntity extends BaseEntity {

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
    
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
    
    public boolean canBeRestored(int daysLimit) {
        if (getDeletedAt() == null) {
            return false;
        }
        return getDeletedAt().isAfter(java.time.LocalDateTime.now().minusDays(daysLimit));
    }
    
    public void restore() {
        setIsDeleted(false);
        setDeletedAt(null);
        setDeletedBy(null);
    }
    
    public void updateContent(String tag, String memo) {
        this.tag = tag;
        this.memo = memo;
    }
}
