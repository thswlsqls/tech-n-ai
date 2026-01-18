package com.tech.n.ai.datasource.aurora.repository.writer.auth;

import com.tech.n.ai.datasource.aurora.entity.auth.AdminEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AdminWriterRepository
 */
@Service
@RequiredArgsConstructor
public class AdminWriterRepository {

    private final AdminWriterJpaRepository adminWriterJpaRepository;

    public AdminEntity save(AdminEntity entity) {
        return adminWriterJpaRepository.save(entity);
    }

    public AdminEntity saveAndFlush(AdminEntity entity) {
        return adminWriterJpaRepository.saveAndFlush(entity);
    }

    public void delete(AdminEntity entity) {
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        adminWriterJpaRepository.save(entity);
    }

    public void deleteById(Long id) {
        AdminEntity entity = adminWriterJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin with id " + id + " does not exist"));
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        adminWriterJpaRepository.save(entity);
    }
}
