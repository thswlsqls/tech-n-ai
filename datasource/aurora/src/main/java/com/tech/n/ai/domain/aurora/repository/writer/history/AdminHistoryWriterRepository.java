package com.tech.n.ai.domain.mariadb.repository.writer.history;

import com.tech.n.ai.domain.mariadb.entity.auth.AdminHistoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AdminHistoryWriterRepository
 */
@Service
@RequiredArgsConstructor
public class AdminHistoryWriterRepository {

    private final AdminHistoryWriterJpaRepository adminHistoryWriterJpaRepository;

    public AdminHistoryEntity save(AdminHistoryEntity entity) {
        return adminHistoryWriterJpaRepository.save(entity);
    }
}
