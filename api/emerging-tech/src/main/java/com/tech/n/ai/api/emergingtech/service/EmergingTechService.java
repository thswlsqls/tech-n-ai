package com.tech.n.ai.api.emergingtech.service;

import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Emerging Tech Service 인터페이스
 */
public interface EmergingTechService {

    /**
     * Emerging Tech 목록 조회
     */
    Page<EmergingTechDocument> findEmergingTechs(String provider, String updateType, String status, Pageable pageable);

    /**
     * Emerging Tech 상세 조회
     */
    EmergingTechDocument findEmergingTechById(String id);

    /**
     * Emerging Tech 검색
     */
    Page<EmergingTechDocument> searchEmergingTech(String query, Pageable pageable);

    /**
     * Emerging Tech 저장 (단건)
     */
    EmergingTechDocument saveEmergingTech(EmergingTechCreateRequest request);

    /**
     * Emerging Tech 상태 변경
     */
    EmergingTechDocument updateStatus(String id, String status);
}
