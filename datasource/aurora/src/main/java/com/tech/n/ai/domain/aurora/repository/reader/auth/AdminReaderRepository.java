package com.tech.n.ai.domain.aurora.repository.reader.auth;

import com.tech.n.ai.domain.aurora.entity.auth.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AdminReaderRepository
 */
@Repository
public interface AdminReaderRepository extends JpaRepository<AdminEntity, Long> {
}
