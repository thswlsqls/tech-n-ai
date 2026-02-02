package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.common.exception.ArchiveDuplicateException;
import com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException;
import com.tech.n.ai.api.archive.common.exception.ArchiveValidationException;
import com.tech.n.ai.api.archive.dto.request.ArchiveCreateRequest;
import com.tech.n.ai.api.archive.dto.request.ArchiveUpdateRequest;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.mariadb.repository.reader.archive.ArchiveReaderRepository;
import com.tech.n.ai.datasource.mariadb.repository.writer.archive.ArchiveWriterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveCommandServiceImpl implements ArchiveCommandService {
    
    private static final int RESTORE_DAYS_LIMIT = 30;
    
    private final ArchiveReaderRepository archiveReaderRepository;
    private final ArchiveWriterRepository archiveWriterRepository;
    
    @Transactional
    @Override
    public ArchiveEntity saveArchive(Long userId, ArchiveCreateRequest request) {
        validateDuplicateArchive(userId, request.itemType(), request.itemId());
        
        ArchiveEntity archive = createArchive(userId, request);
        ArchiveEntity savedArchive = archiveWriterRepository.save(archive);
        
        log.debug("Archive created: id={}, userId={}, itemType={}, itemId={}", 
            savedArchive.getId(), userId, request.itemType(), request.itemId());
        
        return savedArchive;
    }
    
    private void validateDuplicateArchive(Long userId, String itemType, String itemId) {
        archiveReaderRepository.findByUserIdAndItemTypeAndItemIdAndIsDeletedFalse(
            userId, itemType, itemId
        ).ifPresent(archive -> {
            throw new ArchiveDuplicateException("이미 존재하는 아카이브입니다.");
        });
    }
    
    private ArchiveEntity createArchive(Long userId, ArchiveCreateRequest request) {
        ArchiveEntity archive = new ArchiveEntity();
        archive.setUserId(userId);
        archive.setItemType(request.itemType());
        archive.setItemId(request.itemId());
        archive.setTag(request.tag());
        archive.setMemo(request.memo());
        return archive;
    }
    
    @Transactional
    @Override
    public ArchiveEntity updateArchive(Long userId, String archiveTsid, ArchiveUpdateRequest request) {
        Long archiveId = Long.parseLong(archiveTsid);
        ArchiveEntity archive = findAndValidateArchive(userId, archiveId);
        
        archive.updateContent(request.tag(), request.memo());
        ArchiveEntity updatedArchive = archiveWriterRepository.save(archive);
        
        log.debug("Archive updated: id={}, userId={}", archiveId, userId);
        
        return updatedArchive;
    }
    
    private ArchiveEntity findAndValidateArchive(Long userId, Long archiveId) {
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + archiveId));
        
        if (!archive.isOwnedBy(userId)) {
            throw new UnauthorizedException("본인의 아카이브만 접근할 수 있습니다.");
        }
        
        if (Boolean.TRUE.equals(archive.getIsDeleted())) {
            throw new ArchiveNotFoundException("삭제된 아카이브입니다.");
        }
        
        return archive;
    }
    
    @Transactional
    @Override
    public void deleteArchive(Long userId, String archiveTsid) {
        Long archiveId = Long.parseLong(archiveTsid);
        ArchiveEntity archive = findAndValidateArchive(userId, archiveId);
        
        archive.setDeletedBy(userId);
        archiveWriterRepository.delete(archive);
        
        log.debug("Archive deleted: id={}, userId={}", archiveId, userId);
    }
    
    @Transactional
    @Override
    public ArchiveEntity restoreArchive(Long userId, String archiveTsid) {
        Long archiveId = Long.parseLong(archiveTsid);
        ArchiveEntity archive = findDeletedArchive(userId, archiveId);
        validateRestorePeriod(archive);
        
        archive.restore();
        ArchiveEntity restoredArchive = archiveWriterRepository.save(archive);
        
        log.debug("Archive restored: id={}, userId={}", archiveId, userId);
        
        return restoredArchive;
    }
    
    private ArchiveEntity findDeletedArchive(Long userId, Long archiveId) {
        ArchiveEntity archive = archiveReaderRepository.findById(archiveId)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + archiveId));
        
        if (!archive.isOwnedBy(userId)) {
            throw new UnauthorizedException("본인의 아카이브만 접근할 수 있습니다.");
        }
        
        if (!Boolean.TRUE.equals(archive.getIsDeleted())) {
            throw new ArchiveValidationException("삭제되지 않은 아카이브입니다.");
        }
        
        return archive;
    }
    
    private void validateRestorePeriod(ArchiveEntity archive) {
        if (!archive.canBeRestored(RESTORE_DAYS_LIMIT)) {
            throw new ArchiveValidationException(
                "복구 가능 기간이 지났습니다. (" + RESTORE_DAYS_LIMIT + "일 이내만 복구 가능)");
        }
    }
}
