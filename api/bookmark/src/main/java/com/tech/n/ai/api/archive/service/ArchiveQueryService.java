package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.dto.request.ArchiveListRequest;
import com.tech.n.ai.api.archive.dto.request.ArchiveSearchRequest;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;
import org.springframework.data.domain.Page;

/**
 * Archive Query Service 인터페이스
 */
public interface ArchiveQueryService {
    
    /**
     * 아카이브 목록 조회
     * 
     * @param userId 사용자 ID
     * @param request ArchiveListRequest
     * @return 아카이브 목록
     */
    Page<ArchiveEntity> findArchives(Long userId, ArchiveListRequest request);
    
    /**
     * 아카이브 상세 조회
     * 
     * @param userId 사용자 ID
     * @param id 아카이브 ID (TSID)
     * @return ArchiveEntity
     */
    ArchiveEntity findArchiveById(Long userId, Long id);
    
    /**
     * 아카이브 검색
     * 
     * @param userId 사용자 ID
     * @param request ArchiveSearchRequest
     * @return 검색 결과
     */
    Page<ArchiveEntity> searchArchives(Long userId, ArchiveSearchRequest request);
}
