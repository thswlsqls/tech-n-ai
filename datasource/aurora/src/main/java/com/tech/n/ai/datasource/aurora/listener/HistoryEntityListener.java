package com.tech.n.ai.datasource.aurora.listener;

import com.tech.n.ai.datasource.aurora.entity.BaseEntity;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveHistoryEntity;
import com.tech.n.ai.datasource.aurora.entity.auth.AdminEntity;
import com.tech.n.ai.datasource.aurora.entity.auth.AdminHistoryEntity;
import com.tech.n.ai.datasource.aurora.entity.auth.UserEntity;
import com.tech.n.ai.datasource.aurora.entity.auth.UserHistoryEntity;
import com.tech.n.ai.datasource.aurora.util.ApplicationContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * HistoryEntityListener
 */
public class HistoryEntityListener {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof BaseEntity) {
            saveHistory(entity, "INSERT", null, entity);
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof BaseEntity) {
            BaseEntity baseEntity = (BaseEntity) entity;
            Object beforeData = getBeforeData(entity);
            
            if (Boolean.TRUE.equals(baseEntity.getIsDeleted()) && baseEntity.getDeletedAt() != null) {
                if (beforeData instanceof BaseEntity) {
                    BaseEntity beforeEntity = (BaseEntity) beforeData;
                    if (!Boolean.TRUE.equals(beforeEntity.getIsDeleted())) {
                        saveHistory(entity, "DELETE", beforeData, entity);
                        return;
                    }
                }
            }
            
            // 일반 UPDATE
            saveHistory(entity, "UPDATE", beforeData, entity);
        }
    }

    private Object getBeforeData(Object entity) {
        EntityManager entityManager = getEntityManager();
        if (entityManager == null) {
            return null;
        }

        try {
            if (entity instanceof BaseEntity) {
                BaseEntity baseEntity = (BaseEntity) entity;
                Long id = baseEntity.getId();
                if (id != null) {
                    return entityManager.find(entity.getClass(), id);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private void saveHistory(Object entity, String operationType, Object beforeData, Object afterData) {
        EntityManager entityManager = getEntityManager();
        if (entityManager == null) {
            return;
        }

        try {
            String beforeJson = beforeData != null ? objectMapper.writeValueAsString(beforeData) : null;
            String afterJson = afterData != null ? objectMapper.writeValueAsString(afterData) : null;
            Long changedBy = getCurrentUserId();
            LocalDateTime changedAt = LocalDateTime.now();

            if (entity instanceof UserEntity) {
                UserEntity userEntity = (UserEntity) entity;
                UserHistoryEntity history = new UserHistoryEntity();
                history.setUser(userEntity);
                history.setUserId(userEntity.getId());
                history.setOperationType(operationType);
                history.setBeforeData(beforeJson);
                history.setAfterData(afterJson);
                history.setChangedBy(changedBy);
                history.setChangedAt(changedAt);
                entityManager.persist(history);
            } else if (entity instanceof AdminEntity) {
                AdminEntity adminEntity = (AdminEntity) entity;
                AdminHistoryEntity history = new AdminHistoryEntity();
                history.setAdmin(adminEntity);
                history.setAdminId(adminEntity.getId());
                history.setOperationType(operationType);
                history.setBeforeData(beforeJson);
                history.setAfterData(afterJson);
                history.setChangedBy(changedBy);
                history.setChangedAt(changedAt);
                entityManager.persist(history);
            } else if (entity instanceof ArchiveEntity) {
                ArchiveEntity archiveEntity = (ArchiveEntity) entity;
                ArchiveHistoryEntity history = new ArchiveHistoryEntity();
                history.setArchive(archiveEntity);
                history.setArchiveId(archiveEntity.getId());
                history.setOperationType(operationType);
                history.setBeforeData(beforeJson);
                history.setAfterData(afterJson);
                history.setChangedBy(changedBy);
                history.setChangedAt(changedAt);
                entityManager.persist(history);
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            e.printStackTrace();
        }
    }

    private EntityManager getEntityManager() {
        return ApplicationContextProvider.getEntityManager();
    }

    private Long getCurrentUserId() {
        // TODO: SecurityContext에서 현재 사용자 ID 추출
        // 현재는 null 반환
        return null;
    }
}
