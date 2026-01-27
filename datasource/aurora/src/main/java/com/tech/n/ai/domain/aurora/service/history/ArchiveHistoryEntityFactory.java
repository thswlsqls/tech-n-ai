package com.tech.n.ai.datasource.mariadb.service.history;

import com.tech.n.ai.datasource.mariadb.entity.BaseEntity;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveHistoryEntity;
import com.tech.n.ai.datasource.mariadb.repository.writer.history.ArchiveHistoryWriterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ArchiveHistoryEntity 생성을 담당하는 Factory
 */
@Component
@RequiredArgsConstructor
public class ArchiveHistoryEntityFactory implements HistoryEntityFactory {

    private final ArchiveHistoryWriterRepository archiveHistoryWriterRepository;

    @Override
    public void createAndSave(BaseEntity entity, OperationType operationType, 
                              String beforeJson, String afterJson, 
                              Long changedBy, LocalDateTime changedAt) {
        ArchiveEntity archiveEntity = (ArchiveEntity) entity;
        ArchiveHistoryEntity history = new ArchiveHistoryEntity();
        history.setArchive(archiveEntity);
        history.setArchiveId(archiveEntity.getId());
        history.setOperationType(operationType.name());
        history.setBeforeData(beforeJson);
        history.setAfterData(afterJson);
        history.setChangedBy(changedBy);
        history.setChangedAt(changedAt);
        archiveHistoryWriterRepository.save(history);
    }

    @Override
    public boolean supports(BaseEntity entity) {
        return entity instanceof ArchiveEntity;
    }
}
