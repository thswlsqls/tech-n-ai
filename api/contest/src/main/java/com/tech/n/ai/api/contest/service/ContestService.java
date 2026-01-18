package com.tech.n.ai.api.contest.service;

import com.tech.n.ai.api.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contest Service 인터페이스
 */
public interface ContestService {
    
    /**
     * Contest 목록 조회
     * 
     * @param sourceId 출처 ID (optional)
     * @param status 상태 (optional)
     * @param pageable 페이징 정보
     * @return Contest 목록
     */
    Page<ContestDocument> findContests(ObjectId sourceId, String status, Pageable pageable);
    
    /**
     * Contest 상세 조회
     * 
     * @param id Contest ID
     * @return ContestDocument
     */
    ContestDocument findContestById(String id);
    
    /**
     * Contest 검색
     * 
     * @param query 검색어
     * @param pageable 페이징 정보
     * @return Contest 목록
     */
    Page<ContestDocument> searchContest(String query, Pageable pageable);
    
    /**
     * Contest 저장 (단건 처리)
     * 
     * @param request Contest 생성 요청
     * @return 저장된 ContestDocument
     */
    ContestDocument saveContest(ContestCreateRequest request);
}
