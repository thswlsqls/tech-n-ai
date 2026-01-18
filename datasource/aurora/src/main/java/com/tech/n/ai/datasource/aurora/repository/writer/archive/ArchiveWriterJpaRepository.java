package com.tech.n.ai.datasource.aurora.repository.writer.archive;

import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ArchiveWriterJpaRepository
 */
@Repository
public interface ArchiveWriterJpaRepository extends JpaRepository<ArchiveEntity, Long> {
}
