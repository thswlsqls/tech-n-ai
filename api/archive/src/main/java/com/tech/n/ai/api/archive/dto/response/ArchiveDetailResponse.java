package com.tech.n.ai.api.archive.dto.response;

import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.mongodb.document.ArchiveDocument;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

/**
 * 아카이브 상세 조회 응답 DTO
 */
public record ArchiveDetailResponse(
    String archiveTsid,
    String userId,
    String itemType,
    String itemId,
    String itemTitle,
    String itemSummary,
    String tag,
    String memo,
    LocalDateTime archivedAt,
    LocalDateTime itemStartDate,
    LocalDateTime itemEndDate,
    LocalDateTime itemPublishedAt,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    /**
     * ArchiveDocument로부터 ArchiveDetailResponse 생성
     * 
     * @param document ArchiveDocument
     * @return ArchiveDetailResponse
     */
    public static ArchiveDetailResponse from(ArchiveDocument document) {
        if (document == null) {
            return null;
        }
        
        return new ArchiveDetailResponse(
            document.getArchiveTsid(),
            document.getUserId(),
            document.getItemType(),
            document.getItemId() != null ? document.getItemId().toHexString() : null,
            document.getItemTitle(),
            document.getItemSummary(),
            document.getTag(),
            document.getMemo(),
            document.getArchivedAt(),
            document.getItemStartDate(),
            document.getItemEndDate(),
            document.getItemPublishedAt(),
            document.getCreatedAt(),
            document.getCreatedBy(),
            document.getUpdatedAt(),
            document.getUpdatedBy()
        );
    }
    
    /**
     * ArchiveEntity로부터 ArchiveDetailResponse 생성
     * (삭제된 아카이브 조회 시 사용, itemTitle/itemSummary는 null)
     * 
     * @param entity ArchiveEntity
     * @return ArchiveDetailResponse
     */
    public static ArchiveDetailResponse from(ArchiveEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new ArchiveDetailResponse(
            entity.getId() != null ? entity.getId().toString() : null,
            entity.getUserId() != null ? entity.getUserId().toString() : null,
            entity.getItemType(),
            entity.getItemId(),
            null, // itemTitle은 ArchiveEntity에 없음
            null, // itemSummary는 ArchiveEntity에 없음
            entity.getTag(),
            entity.getMemo(),
            entity.getCreatedAt(), // archivedAt은 createdAt으로 대체
            null, // itemStartDate는 ArchiveEntity에 없음
            null, // itemEndDate는 ArchiveEntity에 없음
            null, // itemPublishedAt은 ArchiveEntity에 없음
            entity.getCreatedAt(),
            entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : null,
            entity.getUpdatedAt(),
            entity.getUpdatedBy() != null ? entity.getUpdatedBy().toString() : null
        );
    }
}
