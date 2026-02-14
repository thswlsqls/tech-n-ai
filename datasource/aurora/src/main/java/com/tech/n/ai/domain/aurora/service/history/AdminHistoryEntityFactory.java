package com.tech.n.ai.domain.mariadb.service.history;

import com.tech.n.ai.domain.mariadb.entity.BaseEntity;
import com.tech.n.ai.domain.mariadb.entity.auth.AdminEntity;
import com.tech.n.ai.domain.mariadb.entity.auth.AdminHistoryEntity;
import com.tech.n.ai.domain.mariadb.repository.writer.history.AdminHistoryWriterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AdminHistoryEntity 생성을 담당하는 Factory
 */
@Component
@RequiredArgsConstructor
public class AdminHistoryEntityFactory implements HistoryEntityFactory {

    private final AdminHistoryWriterRepository adminHistoryWriterRepository;

    @Override
    public void createAndSave(BaseEntity entity, OperationType operationType, 
                              String beforeJson, String afterJson, 
                              Long changedBy, LocalDateTime changedAt) {
        AdminEntity adminEntity = (AdminEntity) entity;
        AdminHistoryEntity history = new AdminHistoryEntity();
        history.setAdmin(adminEntity);
        history.setAdminId(adminEntity.getId());
        history.setOperationType(operationType.name());
        history.setBeforeData(beforeJson);
        history.setAfterData(afterJson);
        history.setChangedBy(changedBy);
        history.setChangedAt(changedAt);
        adminHistoryWriterRepository.save(history);
    }

    @Override
    public boolean supports(BaseEntity entity) {
        return entity instanceof AdminEntity;
    }
}
