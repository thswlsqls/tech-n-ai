package com.tech.n.ai.datasource.mariadb.repository.writer.history;

import com.tech.n.ai.datasource.mariadb.entity.auth.UserHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserHistoryWriterJpaRepository
 */
@Repository
public interface UserHistoryWriterJpaRepository extends JpaRepository<UserHistoryEntity, Long> {
}
