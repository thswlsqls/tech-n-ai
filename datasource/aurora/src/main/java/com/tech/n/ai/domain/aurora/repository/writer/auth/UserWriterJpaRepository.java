package com.tech.n.ai.datasource.mariadb.repository.writer.auth;

import com.tech.n.ai.datasource.mariadb.entity.auth.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserWriterJpaRepository
 */
@Repository
public interface UserWriterJpaRepository extends JpaRepository<UserEntity, Long> {
}
