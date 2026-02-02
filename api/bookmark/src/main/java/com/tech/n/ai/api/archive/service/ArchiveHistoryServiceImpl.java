package com.tech.n.ai.api.archive.service;


import com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException;
import com.tech.n.ai.api.archive.common.exception.ArchiveValidationException;
import com.tech.n.ai.api.archive.dto.request.ArchiveHistoryListRequest;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveHistoryEntity;
import com.tech.n.ai.datasource.mariadb.repository.reader.archive.ArchiveHistoryReaderRepository;
import com.tech.n.ai.datasource.mariadb.repository.reader.archive.ArchiveReaderRepository;
import com.tech.n.ai.datasource.mariadb.repository.writer.archive.ArchiveWriterRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveHistoryServiceImpl implements ArchiveHistoryService {
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    private final ArchiveHistoryReaderRepository archiveHistoryReaderRepository;
    private final ArchiveReaderRepository archiveReaderRepository;
    private final ArchiveWriterRepository archiveWriterRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public Page<ArchiveHistoryEntity> findHistory(String userId, String entityId, ArchiveHistoryListRequest request) {
        Long archiveId = Long.parseLong(entityId);
        Long currentUserId = Long.parseLong(userId);
        
        validateArchiveOwnership(archiveId, currentUserId);
        
        // 2. 페이징 처리
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            Sort.by(Sort.Direction.DESC, "changedAt")
        );
        
        // 3. 필터링 조건에 따라 조회
        if (request.operationType() != null && !request.operationType().isBlank()) {
            // operationType 필터링
            if (request.startDate() != null && request.endDate() != null) {
                // 날짜 범위 필터링
                LocalDateTime startDate = LocalDateTime.parse(request.startDate(), ISO_FORMATTER);
                LocalDateTime endDate = LocalDateTime.parse(request.endDate(), ISO_FORMATTER);
                return archiveHistoryReaderRepository.findByArchiveIdAndChangedAtBetween(
                    archiveId, startDate, endDate, pageable
                );
            } else {
                // operationType만 필터링
                return archiveHistoryReaderRepository.findByArchiveIdAndOperationType(
                    archiveId, request.operationType(), pageable
                );
            }
        } else if (request.startDate() != null && request.endDate() != null) {
            // 날짜 범위만 필터링
            LocalDateTime startDate = LocalDateTime.parse(request.startDate(), ISO_FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(request.endDate(), ISO_FORMATTER);
            return archiveHistoryReaderRepository.findByArchiveIdAndChangedAtBetween(
                archiveId, startDate, endDate, pageable
            );
        } else {
            // 필터링 없이 전체 조회
            return archiveHistoryReaderRepository.findByArchiveId(archiveId, pageable);
        }
    }
    
    @Override
    public ArchiveHistoryEntity findHistoryAt(String userId, String entityId, String timestamp) {
        Long archiveId = Long.parseLong(entityId);
        Long currentUserId = Long.parseLong(userId);
        
        validateArchiveOwnership(archiveId, currentUserId);
        
        // 2. 시점 파싱
        LocalDateTime targetTime = LocalDateTime.parse(timestamp, ISO_FORMATTER);
        
        // 3. 특정 시점 이전의 가장 최근 히스토리 조회
        List<ArchiveHistoryEntity> histories = archiveHistoryReaderRepository
            .findTop1ByArchiveIdAndChangedAtLessThanEqualOrderByChangedAtDesc(archiveId, targetTime);
        
        if (histories.isEmpty()) {
            throw new ArchiveNotFoundException("해당 시점의 히스토리를 찾을 수 없습니다: " + timestamp);
        }
        
        return histories.get(0);
    }
    
    @Transactional
    @Override
    public ArchiveEntity restoreFromHistory(String userId, String entityId, String historyId) {
        Long historyIdLong = Long.parseLong(historyId);
        Long archiveId = Long.parseLong(entityId);
        
        ArchiveHistoryEntity history = findHistoryById(historyIdLong);
        ArchiveEntity archive = findArchiveById(archiveId);
        
        Map<String, Object> afterDataMap = parseHistoryData(history, entityId, historyId);
        updateArchiveFromHistory(archive, afterDataMap);
        
        ArchiveEntity updatedArchive = archiveWriterRepository.save(archive);
        
        log.debug("Archive restored from history: archiveId={}, historyId={}, userId={}", 
            entityId, historyId, userId);
        
        return updatedArchive;
    }
    
    private void validateArchiveOwnership(Long archiveId, Long userId) {
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + archiveId));
        
        if (!archive.isOwnedBy(userId)) {
            throw new UnauthorizedException("본인의 아카이브 히스토리만 조회할 수 있습니다.");
        }
    }
    
    private ArchiveHistoryEntity findHistoryById(Long historyId) {
        return archiveHistoryReaderRepository.findByHistoryId(historyId)
            .orElseThrow(() -> new ArchiveNotFoundException("히스토리를 찾을 수 없습니다: " + historyId));
    }
    
    private ArchiveEntity findArchiveById(Long archiveId) {
        return archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + archiveId));
    }
    
    private Map<String, Object> parseHistoryData(ArchiveHistoryEntity history, String entityId, String historyId) {
        if (history.getAfterData() == null) {
            throw new ArchiveValidationException("히스토리에 복구할 데이터가 없습니다.");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> afterDataMap = objectMapper.readValue(history.getAfterData(), Map.class);
            return afterDataMap;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse history after_data: archiveId={}, historyId={}", entityId, historyId, e);
            throw new ArchiveValidationException("히스토리 데이터 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    private void updateArchiveFromHistory(ArchiveEntity archive, Map<String, Object> afterDataMap) {
        String tag = afterDataMap.containsKey("tag") ? (String) afterDataMap.get("tag") : archive.getTag();
        String memo = afterDataMap.containsKey("memo") ? (String) afterDataMap.get("memo") : archive.getMemo();
        archive.updateContent(tag, memo);
    }
}
