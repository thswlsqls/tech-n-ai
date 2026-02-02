package com.tech.n.ai.domain.aurora.repository.writer;

import com.tech.n.ai.domain.aurora.entity.BaseEntity;
import com.tech.n.ai.domain.aurora.service.history.HistoryService;
import com.tech.n.ai.domain.aurora.service.history.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * WriterRepository의 공통 로직을 제공하는 추상 클래스
 * 
 * @param <E> 엔티티 타입 (BaseEntity를 상속)
 */
public abstract class BaseWriterRepository<E extends BaseEntity> {

    protected abstract JpaRepository<E, Long> getJpaRepository();
    protected abstract HistoryService getHistoryService();

    /**
     * 엔티티를 저장하고 History를 기록합니다.
     * 
     * @param entity 저장할 엔티티
     * @return 저장된 엔티티
     */
    public E save(E entity) {
        boolean isNew = entity.getId() == null;
        E beforeData = isNew ? null : getBeforeData(entity.getId());
        
        E saved = getJpaRepository().save(entity);
        
        if (isNew) {
            getHistoryService().saveHistory(saved, OperationType.INSERT, null, saved);
        } else {
            getHistoryService().saveHistory(saved, OperationType.UPDATE, beforeData, saved);
        }
        
        return saved;
    }

    /**
     * 엔티티를 저장하고 즉시 flush하며 History를 기록합니다.
     * 
     * @param entity 저장할 엔티티
     * @return 저장된 엔티티
     */
    public E saveAndFlush(E entity) {
        boolean isNew = entity.getId() == null;
        E beforeData = isNew ? null : getBeforeData(entity.getId());
        
        E saved = getJpaRepository().saveAndFlush(entity);
        
        if (isNew) {
            getHistoryService().saveHistory(saved, OperationType.INSERT, null, saved);
        } else {
            getHistoryService().saveHistory(saved, OperationType.UPDATE, beforeData, saved);
        }
        
        return saved;
    }

    /**
     * 엔티티를 soft delete하고 History를 기록합니다.
     * 
     * @param entity 삭제할 엔티티
     */
    public void delete(E entity) {
        E beforeData = getBeforeData(entity.getId());
        
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        
        E saved = getJpaRepository().save(entity);
        
        if (beforeData != null && !Boolean.TRUE.equals(beforeData.getIsDeleted())) {
            getHistoryService().saveHistory(saved, OperationType.DELETE, beforeData, saved);
        }
    }

    /**
     * ID로 엔티티를 찾아 soft delete하고 History를 기록합니다.
     * 
     * @param id 삭제할 엔티티 ID
     */
    public void deleteById(Long id) {
        E entity = getJpaRepository().findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        getEntityName() + " with id " + id + " does not exist"));
        E beforeData = entity;
        
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        
        E saved = getJpaRepository().save(entity);
        
        if (beforeData != null && !Boolean.TRUE.equals(beforeData.getIsDeleted())) {
            getHistoryService().saveHistory(saved, OperationType.DELETE, beforeData, saved);
        }
    }

    /**
     * 엔티티 ID로 변경 전 데이터를 조회합니다.
     * 
     * @param id 엔티티 ID
     * @return 변경 전 엔티티 데이터 (없으면 null)
     */
    protected E getBeforeData(Long id) {
        return getJpaRepository().findById(id).orElse(null);
    }

    /**
     * 엔티티 이름을 반환합니다. 예외 메시지에 사용됩니다.
     * 
     * @return 엔티티 이름
     */
    protected abstract String getEntityName();
}
