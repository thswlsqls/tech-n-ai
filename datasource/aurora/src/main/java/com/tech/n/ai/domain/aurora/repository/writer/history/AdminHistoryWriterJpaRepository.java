package com.tech.n.ai.domain.mariadb.repository.writer.history;

import com.tech.n.ai.domain.mariadb.entity.auth.AdminHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AdminHistoryWriterJpaRepository
 */
@Repository
public interface AdminHistoryWriterJpaRepository extends JpaRepository<AdminHistoryEntity, Long> {
}
