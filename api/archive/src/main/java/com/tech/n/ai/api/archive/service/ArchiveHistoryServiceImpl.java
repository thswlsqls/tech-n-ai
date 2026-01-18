package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException;
import com.tech.n.ai.api.archive.common.exception.ArchiveValidationException;
import com.tech.n.ai.api.archive.dto.request.ArchiveHistoryListRequest;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.common.kafka.event.ArchiveUpdatedEvent;
import com.tech.n.ai.common.kafka.publisher.EventPublisher;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveHistoryEntity;
import com.tech.n.ai.datasource.aurora.repository.reader.archive.ArchiveHistoryReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.reader.archive.ArchiveReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.writer.archive.ArchiveWriterRepository;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Archive History Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveHistoryServiceImpl implements ArchiveHistoryService {
    
    private static final String KAFKA_TOPIC_ARCHIVE_EVENTS = "archive-events";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    private final ArchiveHistoryReaderRepository archiveHistoryReaderRepository;
    private final ArchiveReaderRepository archiveReaderRepository;
    private final ArchiveWriterRepository archiveWriterRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    @Override
    public Page<ArchiveHistoryEntity> findHistory(String userId, String entityId, ArchiveHistoryListRequest request) {
        // 1. ArchiveEntity 조회 및 권한 검증
        Long archiveId = Long.parseLong(entityId);
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + entityId));
        
        // 권한 검증: 관리자 또는 본인만 조회 가능
        // TODO: 관리자 권한 확인 로직 추가 (현재는 본인만 확인)
        Long currentUserId = Long.parseLong(userId);
        if (!archive.getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("본인의 아카이브 히스토리만 조회할 수 있습니다.");
        }
        
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
        // 1. ArchiveEntity 조회 및 권한 검증
        Long archiveId = Long.parseLong(entityId);
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + entityId));
        
        // 권한 검증: 관리자 또는 본인만 조회 가능
        // TODO: 관리자 권한 확인 로직 추가 (현재는 본인만 확인)
        Long currentUserId = Long.parseLong(userId);
        if (!archive.getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("본인의 아카이브 히스토리만 조회할 수 있습니다.");
        }
        
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
        // 1. 권한 검증: 관리자만 복구 가능
        // TODO: 관리자 권한 확인 로직 추가 (Authentication에서 role 확인)
        // 현재는 Service 레이어에서 권한 확인하지 않고, Controller/Facade에서 확인하도록 함
        
        // 2. 히스토리 엔티티 조회
        Long historyIdLong = Long.parseLong(historyId);
        ArchiveHistoryEntity history = archiveHistoryReaderRepository.findByHistoryId(historyIdLong)
            .orElseThrow(() -> new ArchiveNotFoundException("히스토리를 찾을 수 없습니다: " + historyId));
        
        // 3. ArchiveEntity 조회
        Long archiveId = Long.parseLong(entityId);
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + entityId));
        
        // 4. after_data JSON을 ArchiveEntity로 변환
        if (history.getAfterData() == null) {
            throw new ArchiveValidationException("히스토리에 복구할 데이터가 없습니다.");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> afterDataMap = objectMapper.readValue(
                history.getAfterData(),
                Map.class
            );
            
            // after_data에서 필드 추출하여 ArchiveEntity 업데이트
            if (afterDataMap.containsKey("tag")) {
                archive.setTag((String) afterDataMap.get("tag"));
            }
            if (afterDataMap.containsKey("memo")) {
                archive.setMemo((String) afterDataMap.get("memo"));
            }
            
            ArchiveEntity updatedArchive = archiveWriterRepository.save(archive);
            // HistoryEntityListener가 자동으로 히스토리 저장 (operation_type: UPDATE)
            
            // 5. Kafka 이벤트 발행
            Map<String, Object> updatedFields = new HashMap<>();
            if (afterDataMap.containsKey("tag")) {
                updatedFields.put("tag", afterDataMap.get("tag"));
            }
            if (afterDataMap.containsKey("memo")) {
                updatedFields.put("memo", afterDataMap.get("memo"));
            }
            
            ArchiveUpdatedEvent.ArchiveUpdatedPayload payload = new ArchiveUpdatedEvent.ArchiveUpdatedPayload(
                entityId,
                userId,
                updatedFields
            );
            
            ArchiveUpdatedEvent event = new ArchiveUpdatedEvent(payload);
            eventPublisher.publish(KAFKA_TOPIC_ARCHIVE_EVENTS, event, entityId);
            
            log.debug("Archive restored from history: archiveTsid={}, historyId={}, userId={}", 
                entityId, historyId, userId);
            
            return updatedArchive;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse history after_data: archiveTsid={}, historyId={}", 
                entityId, historyId, e);
            throw new ArchiveValidationException("히스토리 데이터 파싱 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to restore archive from history: archiveTsid={}, historyId={}", 
                entityId, historyId, e);
            throw new ArchiveValidationException("히스토리 복구 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
