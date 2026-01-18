package com.tech.n.ai.datasource.mongodb.repository;

import com.tech.n.ai.datasource.mongodb.document.ArchiveDocument;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ArchiveRepository
 */
@Repository
public interface ArchiveRepository extends MongoRepository<ArchiveDocument, ObjectId> {
    Optional<ArchiveDocument> findByArchiveTsid(String archiveTsid);
    List<ArchiveDocument> findByUserIdOrderByCreatedAtDesc(String userId);
    List<ArchiveDocument> findByUserIdAndItemTypeOrderByCreatedAtDesc(String userId, String itemType);
    Optional<ArchiveDocument> findByUserIdAndItemTypeAndItemId(String userId, String itemType, ObjectId itemId);
    void deleteByArchiveTsid(String archiveTsid);
    
    // 페이징 지원 메서드
    Page<ArchiveDocument> findByUserId(String userId, Pageable pageable);
    Page<ArchiveDocument> findByUserIdAndItemType(String userId, String itemType, Pageable pageable);
    
    // 상세 조회용 (archiveTsid 또는 ObjectId)
    Optional<ArchiveDocument> findByArchiveTsidOrId(String archiveTsid, ObjectId id);
    
    // 검색용 (tag 또는 memo에서 키워드 검색)
    @Query("{ 'user_id': ?0, $or: [ { 'tag': { $regex: ?1, $options: 'i' } }, { 'memo': { $regex: ?1, $options: 'i' } } ] }")
    Page<ArchiveDocument> findByUserIdAndTagContainingOrMemoContaining(String userId, String searchTerm, Pageable pageable);
}
