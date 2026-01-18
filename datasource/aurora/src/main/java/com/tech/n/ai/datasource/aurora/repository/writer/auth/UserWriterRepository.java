package com.tech.n.ai.datasource.aurora.repository.writer.auth;

import com.tech.n.ai.datasource.aurora.entity.auth.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * UserWriterRepository
 */
@Service
@RequiredArgsConstructor
public class UserWriterRepository {

    private final UserWriterJpaRepository userWriterJpaRepository;

    public UserEntity save(UserEntity entity) {
        return userWriterJpaRepository.save(entity);
    }

    public UserEntity saveAndFlush(UserEntity entity) {
        return userWriterJpaRepository.saveAndFlush(entity);
    }

    public void delete(UserEntity entity) {
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        userWriterJpaRepository.save(entity);
    }

    public void deleteById(Long id) {
        UserEntity entity = userWriterJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + id + " does not exist"));
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        userWriterJpaRepository.save(entity);
    }
}
