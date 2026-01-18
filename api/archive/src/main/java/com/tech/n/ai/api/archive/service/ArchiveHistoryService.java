package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.dto.request.ArchiveHistoryListRequest;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveHistoryEntity;
import org.springframework.data.domain.Page;

/**
 * Archive History Service 인터페이스
 */
public interface ArchiveHistoryService {
    
    /**
     * 변경 이력 조회
     * 
     * @param userId 사용자 ID
     * @param entityId 아카이브 엔티티 ID (TSID)
     * @param request ArchiveHistoryListRequest
     * @return 변경 이력 목록
     */
    Page<ArchiveHistoryEntity> findHistory(String userId, String entityId, ArchiveHistoryListRequest request);
    
    /**
     * 특정 시점 데이터 조회
     * 
     * @param userId 사용자 ID
     * @param entityId 아카이브 엔티티 ID (TSID)
     * @param timestamp 시점 (ISO 8601)
     * @return 특정 시점의 히스토리 엔티티
     */
    ArchiveHistoryEntity findHistoryAt(String userId, String entityId, String timestamp);
    
    /**
     * 특정 버전으로 복구
     * 
     * @param userId 사용자 ID
     * @param entityId 아카이브 엔티티 ID (TSID)
     * @param historyId 히스토리 ID (TSID)
     * @return 복구된 ArchiveEntity
     */
    ArchiveEntity restoreFromHistory(String userId, String entityId, String historyId);
}
