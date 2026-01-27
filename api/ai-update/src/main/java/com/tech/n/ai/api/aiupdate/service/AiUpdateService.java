package com.tech.n.ai.api.aiupdate.service;

import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateCreateRequest;
import com.tech.n.ai.datasource.mongodb.document.AiUpdateDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * AI Update Service 인터페이스
 */
public interface AiUpdateService {

    /**
     * AI Update 목록 조회
     */
    Page<AiUpdateDocument> findAiUpdates(String provider, String updateType, String status, Pageable pageable);

    /**
     * AI Update 상세 조회
     */
    AiUpdateDocument findAiUpdateById(String id);

    /**
     * AI Update 검색
     */
    Page<AiUpdateDocument> searchAiUpdate(String query, Pageable pageable);

    /**
     * AI Update 저장 (단건)
     */
    AiUpdateDocument saveAiUpdate(AiUpdateCreateRequest request);

    /**
     * AI Update 상태 변경
     */
    AiUpdateDocument updateStatus(String id, String status);
}
