package com.tech.n.ai.api.archive.facade;

import com.tech.n.ai.api.archive.dto.request.*;
import com.tech.n.ai.api.archive.dto.response.*;
import com.tech.n.ai.api.archive.service.ArchiveCommandService;
import com.tech.n.ai.api.archive.service.ArchiveHistoryService;
import com.tech.n.ai.api.archive.service.ArchiveQueryService;
import com.tech.n.ai.common.core.dto.PageData;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveHistoryEntity;
import com.tech.n.ai.datasource.aurora.repository.reader.archive.ArchiveReaderRepository;
import com.tech.n.ai.datasource.mongodb.document.ArchiveDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Archive Facade
 * Controller와 Service 사이의 중간 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveFacade {
    
    private final ArchiveCommandService archiveCommandService;
    private final ArchiveQueryService archiveQueryService;
    private final ArchiveHistoryService archiveHistoryService;
    private final ArchiveReaderRepository archiveReaderRepository;
    
    /**
     * 아카이브 저장
     * 
     * @param userId 사용자 ID
     * @param request ArchiveCreateRequest
     * @return ArchiveDetailResponse
     */
    public ArchiveDetailResponse saveArchive(Long userId, ArchiveCreateRequest request) {
        ArchiveEntity entity = archiveCommandService.saveArchive(userId, request);
        // CQRS 패턴: 쓰기 작업 후 MongoDB에서 조회 (동기화 지연 고려하여 재시도)
        return getArchiveDetailFromMongoWithRetry(userId, entity.getId().toString());
    }
    
    /**
     * 아카이브 목록 조회
     * 
     * @param userId 사용자 ID
     * @param request ArchiveListRequest
     * @return ArchiveListResponse
     */
    public ArchiveListResponse getArchiveList(Long userId, ArchiveListRequest request) {
        Page<ArchiveDocument> page = archiveQueryService.findArchives(userId.toString(), request);
        
        // Page<ArchiveDocument>를 PageData<ArchiveDetailResponse>로 변환
        List<ArchiveDetailResponse> list = page.getContent().stream()
            .map(ArchiveDetailResponse::from)
            .toList();
        
        PageData<ArchiveDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return ArchiveListResponse.from(pageData);
    }
    
    /**
     * 아카이브 상세 조회
     * 
     * @param userId 사용자 ID
     * @param id 아카이브 ID (archiveTsid 또는 ObjectId)
     * @return ArchiveDetailResponse
     */
    public ArchiveDetailResponse getArchiveDetail(Long userId, String id) {
        ArchiveDocument document = archiveQueryService.findArchiveById(userId.toString(), id);
        return ArchiveDetailResponse.from(document);
    }
    
    /**
     * 아카이브 수정
     * 
     * @param userId 사용자 ID
     * @param id 아카이브 ID (archiveTsid)
     * @param request ArchiveUpdateRequest
     * @return ArchiveDetailResponse
     */
    public ArchiveDetailResponse updateArchive(Long userId, String id, ArchiveUpdateRequest request) {
        archiveCommandService.updateArchive(userId, id, request);
        // CQRS 패턴: 쓰기 작업 후 MongoDB에서 조회 (동기화 지연 고려하여 재시도)
        return getArchiveDetailFromMongoWithRetry(userId, id);
    }
    
    /**
     * 아카이브 삭제
     * 
     * @param userId 사용자 ID
     * @param id 아카이브 ID (archiveTsid)
     */
    public void deleteArchive(Long userId, String id) {
        archiveCommandService.deleteArchive(userId, id);
    }
    
    /**
     * 삭제된 아카이브 목록 조회
     * 
     * @param userId 사용자 ID
     * @param request ArchiveDeletedListRequest
     * @return ArchiveListResponse
     */
    public ArchiveListResponse getDeletedArchives(Long userId, ArchiveDeletedListRequest request) {
        // 삭제된 아카이브는 CQRS 패턴 예외로 Aurora MySQL 사용
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            Sort.by(Sort.Direction.DESC, "deletedAt")
        );
        
        Page<ArchiveEntity> page;
        
        // 복구 가능 기간 필터링 (days)
        if (request.days() != null && request.days() > 0) {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(request.days());
            page = archiveReaderRepository.findDeletedArchivesWithinDays(userId, cutoffDate, pageable);
        } else {
            page = archiveReaderRepository.findByUserIdAndIsDeletedTrue(userId, pageable);
        }
        
        // Page<ArchiveEntity>를 PageData<ArchiveDetailResponse>로 변환
        List<ArchiveDetailResponse> list = page.getContent().stream()
            .map(ArchiveDetailResponse::from)
            .toList();
        
        PageData<ArchiveDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return ArchiveListResponse.from(pageData);
    }
    
    /**
     * 아카이브 복구
     * 
     * @param userId 사용자 ID
     * @param id 아카이브 ID (archiveTsid)
     * @return ArchiveDetailResponse
     */
    public ArchiveDetailResponse restoreArchive(Long userId, String id) {
        archiveCommandService.restoreArchive(userId, id);
        // CQRS 패턴: 쓰기 작업 후 MongoDB에서 조회 (동기화 지연 고려하여 재시도)
        return getArchiveDetailFromMongoWithRetry(userId, id);
    }
    
    /**
     * 아카이브 검색
     * 
     * @param userId 사용자 ID
     * @param request ArchiveSearchRequest
     * @return ArchiveSearchResponse
     */
    public ArchiveSearchResponse searchArchives(Long userId, ArchiveSearchRequest request) {
        Page<ArchiveDocument> page = archiveQueryService.searchArchives(userId.toString(), request);
        
        // Page<ArchiveDocument>를 PageData<ArchiveDetailResponse>로 변환
        List<ArchiveDetailResponse> list = page.getContent().stream()
            .map(ArchiveDetailResponse::from)
            .toList();
        
        PageData<ArchiveDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return ArchiveSearchResponse.from(pageData);
    }
    
    /**
     * 변경 이력 조회
     * 
     * @param userId 사용자 ID
     * @param entityId 아카이브 엔티티 ID (TSID)
     * @param request ArchiveHistoryListRequest
     * @return ArchiveHistoryListResponse
     */
    public ArchiveHistoryListResponse getHistory(Long userId, String entityId, ArchiveHistoryListRequest request) {
        Page<ArchiveHistoryEntity> page = archiveHistoryService.findHistory(userId.toString(), entityId, request);
        
        // Page<ArchiveHistoryEntity>를 PageData<ArchiveHistoryDetailResponse>로 변환
        List<ArchiveHistoryDetailResponse> list = page.getContent().stream()
            .map(ArchiveHistoryDetailResponse::from)
            .toList();
        
        PageData<ArchiveHistoryDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return ArchiveHistoryListResponse.from(pageData);
    }
    
    /**
     * 특정 시점 데이터 조회
     * 
     * @param userId 사용자 ID
     * @param entityId 아카이브 엔티티 ID (TSID)
     * @param timestamp 시점 (ISO 8601)
     * @return ArchiveHistoryDetailResponse
     */
    public ArchiveHistoryDetailResponse getHistoryAt(Long userId, String entityId, String timestamp) {
        ArchiveHistoryEntity history = archiveHistoryService.findHistoryAt(userId.toString(), entityId, timestamp);
        return ArchiveHistoryDetailResponse.from(history);
    }
    
    /**
     * 특정 버전으로 복구
     * 
     * @param userId 사용자 ID
     * @param entityId 아카이브 엔티티 ID (TSID)
     * @param historyId 히스토리 ID (TSID)
     * @return ArchiveDetailResponse
     */
    public ArchiveDetailResponse restoreFromHistory(Long userId, String entityId, String historyId) {
        archiveHistoryService.restoreFromHistory(userId.toString(), entityId, historyId);
        // CQRS 패턴: 쓰기 작업 후 MongoDB에서 조회 (동기화 지연 고려하여 재시도)
        return getArchiveDetailFromMongoWithRetry(userId, entityId);
    }
    
    /**
     * MongoDB에서 아카이브 상세 조회 (동기화 지연 고려하여 재시도)
     * 
     * @param userId 사용자 ID
     * @param id 아카이브 ID (archiveTsid 또는 ObjectId)
     * @return ArchiveDetailResponse
     */
    private ArchiveDetailResponse getArchiveDetailFromMongoWithRetry(Long userId, String id) {
        int maxRetries = 3;
        long retryDelayMs = 100; // 100ms
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                ArchiveDocument document = archiveQueryService.findArchiveById(userId.toString(), id);
                if (document != null) {
                    return ArchiveDetailResponse.from(document);
                }
            } catch (Exception e) {
                log.debug("MongoDB에서 아카이브 조회 실패 (재시도 {}/{}): archiveTsid={}, error={}", 
                    i + 1, maxRetries, id, e.getMessage());
            }
            
            // 마지막 시도가 아니면 대기 후 재시도
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // 재시도 후에도 찾지 못한 경우, ArchiveEntity 기반으로 응답 (fallback)
        // 이는 동기화 지연이 예상보다 긴 경우를 대비한 안전장치
        log.warn("MongoDB에서 아카이브를 찾지 못하여 ArchiveEntity 기반으로 응답: archiveTsid={}", id);
        try {
            Long archiveId = Long.parseLong(id);
            ArchiveEntity entity = archiveReaderRepository.findById(archiveId)
                .orElseThrow(() -> new com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException(
                    "아카이브를 찾을 수 없습니다: " + id));
            return ArchiveDetailResponse.from(entity);
        } catch (NumberFormatException e) {
            throw new com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException(
                "아카이브를 찾을 수 없습니다: " + id);
        }
    }
}
