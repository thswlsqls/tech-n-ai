package com.tech.n.ai.api.archive.facade;

import com.tech.n.ai.api.archive.dto.request.*;
import com.tech.n.ai.api.archive.dto.response.*;
import com.tech.n.ai.api.archive.service.ArchiveCommandService;
import com.tech.n.ai.api.archive.service.ArchiveHistoryService;
import com.tech.n.ai.api.archive.service.ArchiveQueryService;
import com.tech.n.ai.common.core.dto.PageData;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveHistoryEntity;
import com.tech.n.ai.datasource.mariadb.repository.reader.archive.ArchiveReaderRepository;
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
    
    public ArchiveDetailResponse saveArchive(Long userId, ArchiveCreateRequest request) {
        ArchiveEntity entity = archiveCommandService.saveArchive(userId, request);
        return ArchiveDetailResponse.from(entity);
    }
    
    public ArchiveListResponse getArchiveList(Long userId, ArchiveListRequest request) {
        Page<ArchiveEntity> page = archiveQueryService.findArchives(userId, request);
        
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
    
    public ArchiveDetailResponse getArchiveDetail(Long userId, String id) {
        Long archiveId = Long.parseLong(id);
        ArchiveEntity entity = archiveQueryService.findArchiveById(userId, archiveId);
        return ArchiveDetailResponse.from(entity);
    }
    
    public ArchiveDetailResponse updateArchive(Long userId, String id, ArchiveUpdateRequest request) {
        archiveCommandService.updateArchive(userId, id, request);
        Long archiveId = Long.parseLong(id);
        ArchiveEntity entity = archiveQueryService.findArchiveById(userId, archiveId);
        return ArchiveDetailResponse.from(entity);
    }
    
    public void deleteArchive(Long userId, String id) {
        archiveCommandService.deleteArchive(userId, id);
    }
    
    public ArchiveListResponse getDeletedArchives(Long userId, ArchiveDeletedListRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            Sort.by(Sort.Direction.DESC, "deletedAt")
        );
        
        Page<ArchiveEntity> page;
        
        if (request.days() != null && request.days() > 0) {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(request.days());
            page = archiveReaderRepository.findDeletedArchivesWithinDays(userId, cutoffDate, pageable);
        } else {
            page = archiveReaderRepository.findByUserIdAndIsDeletedTrue(userId, pageable);
        }
        
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
    
    public ArchiveDetailResponse restoreArchive(Long userId, String id) {
        archiveCommandService.restoreArchive(userId, id);
        Long archiveId = Long.parseLong(id);
        ArchiveEntity entity = archiveQueryService.findArchiveById(userId, archiveId);
        return ArchiveDetailResponse.from(entity);
    }
    
    public ArchiveSearchResponse searchArchives(Long userId, ArchiveSearchRequest request) {
        Page<ArchiveEntity> page = archiveQueryService.searchArchives(userId, request);
        
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
    
    public ArchiveHistoryListResponse getHistory(Long userId, String entityId, ArchiveHistoryListRequest request) {
        Page<ArchiveHistoryEntity> page = archiveHistoryService.findHistory(userId.toString(), entityId, request);
        
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
    
    public ArchiveHistoryDetailResponse getHistoryAt(Long userId, String entityId, String timestamp) {
        ArchiveHistoryEntity history = archiveHistoryService.findHistoryAt(userId.toString(), entityId, timestamp);
        return ArchiveHistoryDetailResponse.from(history);
    }
    
    public ArchiveDetailResponse restoreFromHistory(Long userId, String entityId, String historyId) {
        archiveHistoryService.restoreFromHistory(userId.toString(), entityId, historyId);
        Long archiveId = Long.parseLong(entityId);
        ArchiveEntity entity = archiveQueryService.findArchiveById(userId, archiveId);
        return ArchiveDetailResponse.from(entity);
    }
}
