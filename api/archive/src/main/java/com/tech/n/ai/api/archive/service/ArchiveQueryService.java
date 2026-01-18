package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.dto.request.ArchiveListRequest;
import com.tech.n.ai.api.archive.dto.request.ArchiveSearchRequest;
import com.tech.n.ai.datasource.mongodb.document.ArchiveDocument;
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
    Page<ArchiveDocument> findArchives(String userId, ArchiveListRequest request);
    
    /**
     * 아카이브 상세 조회
     * 
     * @param userId 사용자 ID
     * @param id 아카이브 ID (archiveTsid 또는 ObjectId)
     * @return ArchiveDocument
     */
    ArchiveDocument findArchiveById(String userId, String id);
    
    /**
     * 아카이브 검색
     * 
     * @param userId 사용자 ID
     * @param request ArchiveSearchRequest
     * @return 검색 결과
     */
    Page<ArchiveDocument> searchArchives(String userId, ArchiveSearchRequest request);
}
