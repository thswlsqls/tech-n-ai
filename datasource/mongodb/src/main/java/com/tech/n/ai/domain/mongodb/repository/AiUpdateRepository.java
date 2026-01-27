package com.tech.n.ai.datasource.mongodb.repository;

import com.tech.n.ai.datasource.mongodb.document.AiUpdateDocument;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AI 업데이트 Repository
 */
@Repository
public interface AiUpdateRepository extends MongoRepository<AiUpdateDocument, ObjectId> {

    /**
     * Provider로 조회
     */
    Page<AiUpdateDocument> findByProvider(String provider, Pageable pageable);

    /**
     * Status로 조회
     */
    Page<AiUpdateDocument> findByStatus(String status, Pageable pageable);

    /**
     * Provider와 Status로 조회
     */
    Page<AiUpdateDocument> findByProviderAndStatus(String provider, String status, Pageable pageable);

    /**
     * 외부 ID로 조회 (중복 체크용)
     */
    Optional<AiUpdateDocument> findByExternalId(String externalId);

    /**
     * 외부 ID 존재 여부 확인
     */
    boolean existsByExternalId(String externalId);

    /**
     * 제목 검색 (대소문자 무시)
     */
    Page<AiUpdateDocument> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
