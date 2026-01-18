package com.tech.n.ai.datasource.aurora.repository.reader.archive;

import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ArchiveReaderRepository
 */
@Repository
public interface ArchiveReaderRepository extends JpaRepository<ArchiveEntity, Long> {
    
    /**
     * userId, itemType, itemId로 조회 (중복 검증용)
     * 
     * @param userId 사용자 ID
     * @param itemType 항목 타입
     * @param itemId 항목 ID
     * @return ArchiveEntity (Optional)
     */
    Optional<ArchiveEntity> findByUserIdAndItemTypeAndItemIdAndIsDeletedFalse(Long userId, String itemType, String itemId);
    
    /**
     * 삭제된 아카이브 조회 (페이징)
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 삭제된 ArchiveEntity 페이지
     */
    Page<ArchiveEntity> findByUserIdAndIsDeletedTrue(Long userId, Pageable pageable);
    
    /**
     * 삭제된 아카이브 조회 (복구 가능 기간 필터링)
     * 
     * @param userId 사용자 ID
     * @param days 복구 가능 기간 (일)
     * @param pageable 페이징 정보
     * @return 삭제된 ArchiveEntity 페이지
     */
    @Query("SELECT a FROM ArchiveEntity a WHERE a.userId = :userId AND a.isDeleted = true " +
           "AND a.deletedAt >= :cutoffDate")
    Page<ArchiveEntity> findDeletedArchivesWithinDays(
        @Param("userId") Long userId,
        @Param("cutoffDate") LocalDateTime cutoffDate,
        Pageable pageable
    );
}
