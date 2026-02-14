package com.tech.n.ai.domain.mariadb.repository.writer.auth;

import com.tech.n.ai.domain.mariadb.entity.auth.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * RefreshTokenWriterJpaRepository
 */
@Repository
public interface RefreshTokenWriterJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
}
