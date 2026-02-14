package com.tech.n.ai.api.emergingtech.service;

import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Emerging Tech 조회 서비스
 */
public interface EmergingTechQueryService {

    /**
     * 필터 조건에 따른 목록 조회
     */
    Page<EmergingTechDocument> findEmergingTechs(
        String provider, String updateType, String status,
        String sourceType, String startDate, String endDate,
        Pageable pageable);

    /**
     * ID로 단건 조회
     */
    EmergingTechDocument findEmergingTechById(String id);

    /**
     * 제목 키워드 검색
     */
    Page<EmergingTechDocument> searchEmergingTech(String query, Pageable pageable);
}
