package com.tech.n.ai.api.archive.dto.response;

import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveHistoryEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 아카이브 히스토리 상세 조회 응답 DTO
 */
public record ArchiveHistoryDetailResponse(
    String historyId,
    String entityId,
    String operationType,
    Map<String, Object> beforeData,
    Map<String, Object> afterData,
    String changedBy,
    LocalDateTime changedAt,
    String changeReason
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * ArchiveHistoryEntity로부터 ArchiveHistoryDetailResponse 생성
     * 
     * @param entity ArchiveHistoryEntity
     * @return ArchiveHistoryDetailResponse
     */
    @SuppressWarnings("unchecked")
    public static ArchiveHistoryDetailResponse from(ArchiveHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Map<String, Object> beforeDataMap = null;
        Map<String, Object> afterDataMap = null;
        
        try {
            if (entity.getBeforeData() != null) {
                beforeDataMap = objectMapper.readValue(
                    entity.getBeforeData(),
                    Map.class
                );
            }
            if (entity.getAfterData() != null) {
                afterDataMap = objectMapper.readValue(
                    entity.getAfterData(),
                    Map.class
                );
            }
        } catch (Exception e) {
            // JSON 파싱 실패 시 null로 설정
        }
        
        return new ArchiveHistoryDetailResponse(
            entity.getHistoryId() != null ? entity.getHistoryId().toString() : null,
            entity.getArchiveId() != null ? entity.getArchiveId().toString() : null,
            entity.getOperationType(),
            beforeDataMap,
            afterDataMap,
            entity.getChangedBy() != null ? entity.getChangedBy().toString() : null,
            entity.getChangedAt(),
            entity.getChangeReason()
        );
    }
}
