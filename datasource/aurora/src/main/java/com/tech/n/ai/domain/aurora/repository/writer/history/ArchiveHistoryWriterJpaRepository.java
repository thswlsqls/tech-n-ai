package com.tech.n.ai.datasource.mariadb.repository.writer.history;

import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ArchiveHistoryWriterJpaRepository
 */
@Repository
public interface ArchiveHistoryWriterJpaRepository extends JpaRepository<ArchiveHistoryEntity, Long> {
}
