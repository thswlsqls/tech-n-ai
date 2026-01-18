package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.ArchiveCreatedEvent;
import com.tech.n.ai.common.kafka.event.ArchiveDeletedEvent;
import com.tech.n.ai.common.kafka.event.ArchiveRestoredEvent;
import com.tech.n.ai.common.kafka.event.ArchiveUpdatedEvent;
import com.tech.n.ai.datasource.mongodb.document.ArchiveDocument;
import com.tech.n.ai.datasource.mongodb.repository.ArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Archive 동기화 서비스 구현 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveSyncServiceImpl implements ArchiveSyncService {
    
    private final ArchiveRepository archiveRepository;
    
    @Override
    public void syncArchiveCreated(ArchiveCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: archiveTsid로 조회하여 없으면 생성, 있으면 업데이트
            ArchiveDocument document = archiveRepository
                .findByArchiveTsid(payload.archiveTsid())
                .orElse(new ArchiveDocument());
            
            document.setArchiveTsid(payload.archiveTsid());
            document.setUserId(payload.userId());
            document.setItemType(payload.itemType());
            document.setItemId(new ObjectId(payload.itemId()));
            document.setItemTitle(payload.itemTitle());
            document.setItemSummary(payload.itemSummary());
            document.setTag(payload.tag());
            document.setMemo(payload.memo());
            document.setArchivedAt(convertToLocalDateTime(payload.archivedAt()));
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            archiveRepository.save(document);
            
            log.debug("Successfully synced ArchiveCreatedEvent: archiveTsid={}, userId={}", 
                payload.archiveTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveCreatedEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveCreatedEvent", e);
        }
    }
    
    @Override
    public void syncArchiveUpdated(ArchiveUpdatedEvent event) {
        try {
            var payload = event.payload();
            var updatedFields = payload.updatedFields();
            
            // archiveTsid로 Document 조회
            ArchiveDocument document = archiveRepository
                .findByArchiveTsid(payload.archiveTsid())
                .orElseThrow(() -> new RuntimeException(
                    "ArchiveDocument not found: archiveTsid=" + payload.archiveTsid()));
            
            // updatedFields를 Document 필드에 매핑 (부분 업데이트)
            updateDocumentFields(document, updatedFields);
            document.setUpdatedAt(LocalDateTime.now());
            
            archiveRepository.save(document);
            
            log.debug("Successfully synced ArchiveUpdatedEvent: archiveTsid={}, updatedFields={}", 
                payload.archiveTsid(), updatedFields.keySet());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveUpdatedEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveUpdatedEvent", e);
        }
    }
    
    @Override
    public void syncArchiveDeleted(ArchiveDeletedEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제
            archiveRepository.deleteByArchiveTsid(payload.archiveTsid());
            
            log.debug("Successfully synced ArchiveDeletedEvent: archiveTsid={}, userId={}", 
                payload.archiveTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveDeletedEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveDeletedEvent", e);
        }
    }
    
    @Override
    public void syncArchiveRestored(ArchiveRestoredEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
            ArchiveDocument document = new ArchiveDocument();
            document.setArchiveTsid(payload.archiveTsid());
            document.setUserId(payload.userId());
            document.setItemType(payload.itemType());
            document.setItemId(new ObjectId(payload.itemId()));
            document.setItemTitle(payload.itemTitle());
            document.setItemSummary(payload.itemSummary());
            document.setTag(payload.tag());
            document.setMemo(payload.memo());
            document.setArchivedAt(convertToLocalDateTime(payload.archivedAt()));
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            archiveRepository.save(document);
            
            log.debug("Successfully synced ArchiveRestoredEvent: archiveTsid={}, userId={}", 
                payload.archiveTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveRestoredEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveRestoredEvent", e);
        }
    }
    
    /**
     * updatedFields를 Document 필드에 매핑 (부분 업데이트)
     * 
     * 주의: itemTitle, itemSummary는 ArchiveEntity에 없는 필드이므로
     * ArchiveUpdatedEvent의 updatedFields에 포함될 수 없습니다.
     * 이 필드들은 원본 아이템(ContestDocument/NewsArticleDocument) 변경 시
     * 별도의 동기화 메커니즘으로 업데이트되어야 합니다.
     * 
     * @param document 대상 Document
     * @param updatedFields 업데이트할 필드 맵 (tag, memo만 가능)
     */
    private void updateDocumentFields(ArchiveDocument document, Map<String, Object> updatedFields) {
        for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            try {
                switch (fieldName) {
                    // itemTitle, itemSummary는 ArchiveEntity에 없는 필드이므로 제외
                    // 원본 아이템 변경 시 별도 동기화 메커니즘 필요
                    case "tag":
                        document.setTag((String) value);
                        break;
                    case "memo":
                        document.setMemo((String) value);
                        break;
                    default:
                        log.warn("Unknown field in updatedFields: {} (itemTitle, itemSummary are not supported as they are not in ArchiveEntity)", fieldName);
                }
            } catch (ClassCastException e) {
                log.warn("Type mismatch for field {}: expected String, got {}", fieldName, value.getClass().getName());
            }
        }
    }
    
    /**
     * Instant를 LocalDateTime으로 변환
     * 
     * @param instant Instant 객체
     * @return LocalDateTime 객체
     */
    private LocalDateTime convertToLocalDateTime(Instant instant) {
        return instant != null 
            ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            : null;
    }
}
