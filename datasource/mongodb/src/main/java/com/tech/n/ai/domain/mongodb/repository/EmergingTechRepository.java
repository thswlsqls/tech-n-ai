package com.tech.n.ai.domain.mongodb.repository;

import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Emerging Tech Repository
 */
@Repository
public interface EmergingTechRepository extends MongoRepository<EmergingTechDocument, ObjectId> {

    /**
     * Provider로 조회
     */
    Page<EmergingTechDocument> findByProvider(String provider, Pageable pageable);

    /**
     * Status로 조회
     */
    Page<EmergingTechDocument> findByStatus(String status, Pageable pageable);

    /**
     * Provider와 Status로 조회
     */
    Page<EmergingTechDocument> findByProviderAndStatus(String provider, String status, Pageable pageable);

    /**
     * 외부 ID로 조회 (중복 체크용)
     */
    Optional<EmergingTechDocument> findByExternalId(String externalId);

    /**
     * 외부 ID 존재 여부 확인
     */
    boolean existsByExternalId(String externalId);

    /**
     * URL로 조회 (중복 체크용)
     */
    Optional<EmergingTechDocument> findByUrl(String url);

    /**
     * URL 존재 여부 확인
     */
    boolean existsByUrl(String url);

    /**
     * 제목 검색 (대소문자 무시)
     */
    Page<EmergingTechDocument> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
