package com.tech.n.ai.datasource.mariadb.repository.writer.archive;

import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.mariadb.repository.reader.auth.UserReaderRepository;
import com.tech.n.ai.datasource.mariadb.repository.writer.BaseWriterRepository;
import com.tech.n.ai.datasource.mariadb.service.history.HistoryService;
import com.tech.n.ai.datasource.mariadb.service.history.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * ArchiveWriterRepository
 */
@Service
@RequiredArgsConstructor
public class ArchiveWriterRepository extends BaseWriterRepository<ArchiveEntity> {

    private final ArchiveWriterJpaRepository archiveWriterJpaRepository;
    private final UserReaderRepository userReaderRepository;
    private final HistoryService historyService;

    @Override
    protected JpaRepository<ArchiveEntity, Long> getJpaRepository() {
        return archiveWriterJpaRepository;
    }

    @Override
    protected HistoryService getHistoryService() {
        return historyService;
    }

    @Override
    protected String getEntityName() {
        return "Archive";
    }

    @Override
    public ArchiveEntity save(ArchiveEntity entity) {
        validateUserId(entity);
        return super.save(entity);
    }

    @Override
    public ArchiveEntity saveAndFlush(ArchiveEntity entity) {
        validateUserId(entity);
        return super.saveAndFlush(entity);
    }

    private void validateUserId(ArchiveEntity entity) {
        if (entity.getUserId() != null && !userReaderRepository.existsById(entity.getUserId())) {
            throw new IllegalArgumentException("User with id " + entity.getUserId() + " does not exist");
        }
    }
}
