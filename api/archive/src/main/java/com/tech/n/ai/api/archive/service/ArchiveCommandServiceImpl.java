package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.common.exception.ArchiveDuplicateException;
import com.tech.n.ai.api.archive.common.exception.ArchiveItemNotFoundException;
import com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException;
import com.tech.n.ai.api.archive.common.exception.ArchiveValidationException;
import com.tech.n.ai.api.archive.dto.request.ArchiveCreateRequest;
import com.tech.n.ai.api.archive.dto.request.ArchiveUpdateRequest;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.common.kafka.event.ArchiveCreatedEvent;
import com.tech.n.ai.common.kafka.event.ArchiveDeletedEvent;
import com.tech.n.ai.common.kafka.event.ArchiveRestoredEvent;
import com.tech.n.ai.common.kafka.event.ArchiveUpdatedEvent;
import com.tech.n.ai.common.kafka.publisher.EventPublisher;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.aurora.repository.reader.archive.ArchiveReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.writer.archive.ArchiveWriterRepository;
import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import com.tech.n.ai.datasource.mongodb.document.NewsArticleDocument;
import com.tech.n.ai.datasource.mongodb.repository.ContestRepository;
import com.tech.n.ai.datasource.mongodb.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Archive Command Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveCommandServiceImpl implements ArchiveCommandService {
    
    private static final String KAFKA_TOPIC_ARCHIVE_EVENTS = "archive-events";
    
    private final ArchiveReaderRepository archiveReaderRepository;
    private final ArchiveWriterRepository archiveWriterRepository;
    private final ContestRepository contestRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final EventPublisher eventPublisher;
    
    @Transactional
    @Override
    public ArchiveEntity saveArchive(Long userId, ArchiveCreateRequest request) {
        // 1. 원본 아이템 존재 여부 확인
        String itemTitle;
        String itemSummary;
        ObjectId itemId = new ObjectId(request.itemId());
        
        if ("CONTEST".equals(request.itemType())) {
            ContestDocument contest = contestRepository.findById(itemId)
                .orElseThrow(() -> new ArchiveItemNotFoundException("대회를 찾을 수 없습니다: " + request.itemId()));
            itemTitle = contest.getTitle();
            itemSummary = contest.getDescription();
        } else if ("NEWS_ARTICLE".equals(request.itemType())) {
            NewsArticleDocument news = newsArticleRepository.findById(itemId)
                .orElseThrow(() -> new ArchiveItemNotFoundException("뉴스 기사를 찾을 수 없습니다: " + request.itemId()));
            itemTitle = news.getTitle();
            itemSummary = news.getSummary();
        } else {
            throw new ArchiveValidationException("유효하지 않은 항목 타입입니다: " + request.itemType());
        }
        
        // 2. 중복 검증
        archiveReaderRepository.findByUserIdAndItemTypeAndItemIdAndIsDeletedFalse(
            userId, request.itemType(), request.itemId()
        ).ifPresent(archive -> {
            throw new ArchiveDuplicateException("이미 존재하는 아카이브입니다.");
        });
        
        // 3. ArchiveEntity 생성
        ArchiveEntity archive = new ArchiveEntity();
        archive.setUserId(userId);
        archive.setItemType(request.itemType());
        archive.setItemId(request.itemId());
        archive.setTag(request.tag());
        archive.setMemo(request.memo());
        // BaseEntity의 created_at, created_by는 자동 설정
        
        ArchiveEntity savedArchive = archiveWriterRepository.save(archive);
        // HistoryEntityListener가 자동으로 히스토리 저장 (operation_type: INSERT)
        
        // 4. Kafka 이벤트 발행
        ArchiveCreatedEvent.ArchiveCreatedPayload payload = new ArchiveCreatedEvent.ArchiveCreatedPayload(
            savedArchive.getId().toString(),  // archiveTsid
            userId.toString(),                 // userId
            request.itemType(),
            request.itemId(),
            itemTitle,
            itemSummary,
            request.tag(),
            request.memo(),
            Instant.now()                     // archivedAt
        );
        
        ArchiveCreatedEvent event = new ArchiveCreatedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_ARCHIVE_EVENTS, event, savedArchive.getId().toString());
        
        log.debug("Archive created: archiveTsid={}, userId={}, itemType={}, itemId={}", 
            savedArchive.getId(), userId, request.itemType(), request.itemId());
        
        return savedArchive;
    }
    
    @Transactional
    @Override
    public ArchiveEntity updateArchive(Long userId, String archiveTsid, ArchiveUpdateRequest request) {
        // 1. ArchiveEntity 조회
        Long archiveId = Long.parseLong(archiveTsid);
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + archiveTsid));
        
        // 2. 권한 검증
        if (!archive.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 아카이브만 수정할 수 있습니다.");
        }
        
        // 3. Soft Delete 확인
        if (Boolean.TRUE.equals(archive.getIsDeleted())) {
            throw new ArchiveNotFoundException("삭제된 아카이브는 수정할 수 없습니다.");
        }
        
        // 4. tag, memo만 수정 (ArchiveEntity에 있는 필드만)
        archive.setTag(request.tag());
        archive.setMemo(request.memo());
        
        ArchiveEntity updatedArchive = archiveWriterRepository.save(archive);
        // HistoryEntityListener가 자동으로 히스토리 저장 (operation_type: UPDATE)
        
        // 5. Kafka 이벤트 발행
        Map<String, Object> updatedFields = new HashMap<>();
        if (request.tag() != null) {
            updatedFields.put("tag", request.tag());
        }
        if (request.memo() != null) {
            updatedFields.put("memo", request.memo());
        }
        
        // 주의: itemTitle, itemSummary는 ArchiveEntity에 없는 필드이므로 updatedFields에 포함하지 않음
        ArchiveUpdatedEvent.ArchiveUpdatedPayload payload = new ArchiveUpdatedEvent.ArchiveUpdatedPayload(
            archiveTsid,
            userId.toString(),
            updatedFields
        );
        
        ArchiveUpdatedEvent event = new ArchiveUpdatedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_ARCHIVE_EVENTS, event, archiveTsid);
        
        log.debug("Archive updated: archiveTsid={}, userId={}, updatedFields={}", 
            archiveTsid, userId, updatedFields);
        
        return updatedArchive;
    }
    
    @Transactional
    @Override
    public void deleteArchive(Long userId, String archiveTsid) {
        // 1. ArchiveEntity 조회
        Long archiveId = Long.parseLong(archiveTsid);
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + archiveTsid));
        
        // 2. 권한 검증
        if (!archive.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 아카이브만 삭제할 수 있습니다.");
        }
        
        // 3. Soft Delete 확인
        if (Boolean.TRUE.equals(archive.getIsDeleted())) {
            throw new ArchiveNotFoundException("이미 삭제된 아카이브입니다.");
        }
        
        // 4. Soft Delete
        archive.setDeletedBy(userId);
        archiveWriterRepository.delete(archive);
        // HistoryEntityListener가 자동으로 히스토리 저장 (operation_type: DELETE)
        
        // 5. Kafka 이벤트 발행
        ArchiveDeletedEvent.ArchiveDeletedPayload payload = new ArchiveDeletedEvent.ArchiveDeletedPayload(
            archiveTsid,
            userId.toString(),
            Instant.now()  // deletedAt
        );
        
        ArchiveDeletedEvent event = new ArchiveDeletedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_ARCHIVE_EVENTS, event, archiveTsid);
        
        log.debug("Archive deleted: archiveTsid={}, userId={}", archiveTsid, userId);
    }
    
    @Transactional
    @Override
    public ArchiveEntity restoreArchive(Long userId, String archiveTsid) {
        // 1. ArchiveEntity 조회
        Long archiveId = Long.parseLong(archiveTsid);
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + archiveTsid));
        
        // 2. 권한 검증
        if (!archive.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 아카이브만 복구할 수 있습니다.");
        }
        
        // 3. Soft Delete 확인
        if (!Boolean.TRUE.equals(archive.getIsDeleted())) {
            throw new ArchiveValidationException("삭제되지 않은 아카이브입니다.");
        }
        
        // 4. 복구 기간 검증 (30일 이내)
        if (archive.getDeletedAt() != null) {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            if (archive.getDeletedAt().isBefore(thirtyDaysAgo)) {
                throw new ArchiveValidationException("복구 가능 기간이 지났습니다. (30일 이내만 복구 가능)");
            }
        }
        
        // 5. 원본 아이템 정보 재조회 (itemTitle, itemSummary - ArchiveEntity에 없는 필드)
        String itemTitle;
        String itemSummary;
        ObjectId itemId = new ObjectId(archive.getItemId());
        
        if ("CONTEST".equals(archive.getItemType())) {
            ContestDocument contest = contestRepository.findById(itemId)
                .orElseThrow(() -> new ArchiveItemNotFoundException("대회를 찾을 수 없습니다: " + archive.getItemId()));
            itemTitle = contest.getTitle();
            itemSummary = contest.getDescription();
        } else if ("NEWS_ARTICLE".equals(archive.getItemType())) {
            NewsArticleDocument news = newsArticleRepository.findById(itemId)
                .orElseThrow(() -> new ArchiveItemNotFoundException("뉴스 기사를 찾을 수 없습니다: " + archive.getItemId()));
            itemTitle = news.getTitle();
            itemSummary = news.getSummary();
        } else {
            throw new ArchiveValidationException("유효하지 않은 항목 타입입니다: " + archive.getItemType());
        }
        
        // 6. 복구: is_deleted = false, deleted_at = null, deleted_by = null
        archive.setIsDeleted(false);
        archive.setDeletedAt(null);
        archive.setDeletedBy(null);
        
        ArchiveEntity restoredArchive = archiveWriterRepository.save(archive);
        // HistoryEntityListener가 자동으로 히스토리 저장 (operation_type: UPDATE)
        
        // 7. Kafka 이벤트 발행
        ArchiveRestoredEvent.ArchiveRestoredPayload payload = new ArchiveRestoredEvent.ArchiveRestoredPayload(
            archiveTsid,
            userId.toString(),
            archive.getItemType(),
            archive.getItemId(),
            itemTitle,
            itemSummary,
            archive.getTag(),
            archive.getMemo(),
            archive.getCreatedAt() != null ? archive.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : Instant.now()  // archivedAt
        );
        
        ArchiveRestoredEvent event = new ArchiveRestoredEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_ARCHIVE_EVENTS, event, archiveTsid);
        
        log.debug("Archive restored: archiveTsid={}, userId={}", archiveTsid, userId);
        
        return restoredArchive;
    }
}
