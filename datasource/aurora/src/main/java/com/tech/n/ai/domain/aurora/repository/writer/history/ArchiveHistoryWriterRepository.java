package com.tech.n.ai.datasource.mariadb.repository.writer.history;

import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveHistoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ArchiveHistoryWriterRepository
 */
@Service
@RequiredArgsConstructor
public class ArchiveHistoryWriterRepository {

    private final ArchiveHistoryWriterJpaRepository archiveHistoryWriterJpaRepository;

    public ArchiveHistoryEntity save(ArchiveHistoryEntity entity) {
        return archiveHistoryWriterJpaRepository.save(entity);
    }
}
