package com.tech.n.ai.datasource.aurora.repository.writer.auth;

import com.tech.n.ai.datasource.aurora.entity.auth.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * RefreshTokenWriterJpaRepository
 */
@Repository
public interface RefreshTokenWriterJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
}
