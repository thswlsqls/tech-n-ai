package com.tech.n.ai.datasource.mariadb.repository.writer.auth;

import com.tech.n.ai.datasource.mariadb.entity.auth.EmailVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * EmailVerificationWriterJpaRepository
 */
@Repository
public interface EmailVerificationWriterJpaRepository extends JpaRepository<EmailVerificationEntity, Long> {
}
