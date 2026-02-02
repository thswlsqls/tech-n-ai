package com.tech.n.ai.api.archive.dto.response;

import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;

import java.time.LocalDateTime;

/**
 * 아카이브 상세 조회 응답 DTO
 */
public record ArchiveDetailResponse(
    String archiveTsid,
    String userId,
    String itemType,
    String itemId,
    String tag,
    String memo,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    public static ArchiveDetailResponse from(ArchiveEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new ArchiveDetailResponse(
            entity.getId() != null ? entity.getId().toString() : null,
            entity.getUserId() != null ? entity.getUserId().toString() : null,
            entity.getItemType(),
            entity.getItemId(),
            entity.getTag(),
            entity.getMemo(),
            entity.getCreatedAt(),
            entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : null,
            entity.getUpdatedAt(),
            entity.getUpdatedBy() != null ? entity.getUpdatedBy().toString() : null
        );
    }
}
