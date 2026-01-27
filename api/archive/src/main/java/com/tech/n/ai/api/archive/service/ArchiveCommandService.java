package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.dto.request.ArchiveCreateRequest;
import com.tech.n.ai.api.archive.dto.request.ArchiveUpdateRequest;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;

/**
 * Archive Command Service 인터페이스
 * Aurora MySQL을 사용하는 쓰기 작업을 처리합니다.
 */
public interface ArchiveCommandService {
    
    /**
     * 아카이브 저장
     * 
     * @param userId 사용자 ID
     * @param request 아카이브 생성 요청
     * @return 저장된 ArchiveEntity
     */
    ArchiveEntity saveArchive(Long userId, ArchiveCreateRequest request);
    
    /**
     * 아카이브 수정
     * 
     * @param userId 사용자 ID
     * @param archiveTsid 아카이브 TSID
     * @param request 아카이브 수정 요청
     * @return 수정된 ArchiveEntity
     */
    ArchiveEntity updateArchive(Long userId, String archiveTsid, ArchiveUpdateRequest request);
    
    /**
     * 아카이브 삭제 (Soft Delete)
     * 
     * @param userId 사용자 ID
     * @param archiveTsid 아카이브 TSID
     */
    void deleteArchive(Long userId, String archiveTsid);
    
    /**
     * 아카이브 복구
     * 
     * @param userId 사용자 ID
     * @param archiveTsid 아카이브 TSID
     * @return 복구된 ArchiveEntity
     */
    ArchiveEntity restoreArchive(Long userId, String archiveTsid);
}
