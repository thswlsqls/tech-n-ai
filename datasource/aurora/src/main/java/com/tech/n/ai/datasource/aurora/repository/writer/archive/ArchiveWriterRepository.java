package com.tech.n.ai.datasource.aurora.repository.writer.archive;

import com.tech.n.ai.datasource.aurora.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.aurora.repository.reader.auth.UserReaderRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ArchiveWriterRepository
 */
@Service
@RequiredArgsConstructor
public class ArchiveWriterRepository {

    private final ArchiveWriterJpaRepository archiveWriterJpaRepository;
    private final UserReaderRepository userReaderRepository;

    public ArchiveEntity save(ArchiveEntity entity) {
        // 스키마 간 Foreign Key 검증: userId가 auth.users 테이블에 존재하는지 확인
        if (entity.getUserId() != null && !userReaderRepository.existsById(entity.getUserId())) {
            throw new IllegalArgumentException("User with id " + entity.getUserId() + " does not exist");
        }
        return archiveWriterJpaRepository.save(entity);
    }

    public ArchiveEntity saveAndFlush(ArchiveEntity entity) {
        // 스키마 간 Foreign Key 검증: userId가 auth.users 테이블에 존재하는지 확인
        if (entity.getUserId() != null && !userReaderRepository.existsById(entity.getUserId())) {
            throw new IllegalArgumentException("User with id " + entity.getUserId() + " does not exist");
        }
        return archiveWriterJpaRepository.saveAndFlush(entity);
    }

    public void delete(ArchiveEntity entity) {
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        archiveWriterJpaRepository.save(entity);
    }

    public void deleteById(Long id) {
        ArchiveEntity entity = archiveWriterJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Archive with id " + id + " does not exist"));
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        archiveWriterJpaRepository.save(entity);
    }
}
