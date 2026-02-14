package com.tech.n.ai.domain.mariadb.repository.writer.auth;

import com.tech.n.ai.domain.mariadb.entity.auth.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AdminWriterJpaRepository
 */
@Repository
public interface AdminWriterJpaRepository extends JpaRepository<AdminEntity, Long> {
}
