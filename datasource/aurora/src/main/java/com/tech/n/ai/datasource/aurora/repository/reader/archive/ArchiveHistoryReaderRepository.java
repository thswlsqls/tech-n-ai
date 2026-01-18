package com.tech.n.ai.datasource.aurora.repository.reader.archive;

import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ArchiveHistoryReaderRepository
 */
@Repository
public interface ArchiveHistoryReaderRepository extends JpaRepository<ArchiveHistoryEntity, Long> {
    
    /**
     * archiveId로 히스토리 조회 (페이징)
     * 
     * @param archiveId 아카이브 ID
     * @param pageable 페이징 정보
     * @return ArchiveHistoryEntity 페이지
     */
    Page<ArchiveHistoryEntity> findByArchiveId(Long archiveId, Pageable pageable);
    
    /**
     * archiveId와 operationType으로 히스토리 조회 (페이징)
     * 
     * @param archiveId 아카이브 ID
     * @param operationType 작업 타입
     * @param pageable 페이징 정보
     * @return ArchiveHistoryEntity 페이지
     */
    Page<ArchiveHistoryEntity> findByArchiveIdAndOperationType(Long archiveId, String operationType, Pageable pageable);
    
    /**
     * archiveId와 날짜 범위로 히스토리 조회 (페이징)
     * 
     * @param archiveId 아카이브 ID
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이징 정보
     * @return ArchiveHistoryEntity 페이지
     */
    @Query("SELECT h FROM ArchiveHistoryEntity h WHERE h.archiveId = :archiveId " +
           "AND h.changedAt >= :startDate AND h.changedAt <= :endDate")
    Page<ArchiveHistoryEntity> findByArchiveIdAndChangedAtBetween(
        @Param("archiveId") Long archiveId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * 특정 시점 이전의 가장 최근 히스토리 조회
     * 
     * @param archiveId 아카이브 ID
     * @param timestamp 시점
     * @return ArchiveHistoryEntity (Optional)
     */
    @Query("SELECT h FROM ArchiveHistoryEntity h WHERE h.archiveId = :archiveId " +
           "AND h.changedAt <= :timestamp ORDER BY h.changedAt DESC")
    List<ArchiveHistoryEntity> findTop1ByArchiveIdAndChangedAtLessThanEqualOrderByChangedAtDesc(
        @Param("archiveId") Long archiveId,
        @Param("timestamp") LocalDateTime timestamp
    );
    
    /**
     * historyId로 히스토리 조회
     * 
     * @param historyId 히스토리 ID
     * @return ArchiveHistoryEntity (Optional)
     */
    Optional<ArchiveHistoryEntity> findByHistoryId(Long historyId);
}
