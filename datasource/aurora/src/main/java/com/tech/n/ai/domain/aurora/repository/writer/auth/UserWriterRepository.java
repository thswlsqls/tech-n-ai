package com.tech.n.ai.datasource.mariadb.repository.writer.auth;

import com.tech.n.ai.datasource.mariadb.entity.auth.UserEntity;
import com.tech.n.ai.datasource.mariadb.repository.writer.BaseWriterRepository;
import com.tech.n.ai.datasource.mariadb.service.history.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * UserWriterRepository
 */
@Service
@RequiredArgsConstructor
public class UserWriterRepository extends BaseWriterRepository<UserEntity> {

    private final UserWriterJpaRepository userWriterJpaRepository;
    private final HistoryService historyService;

    @Override
    protected JpaRepository<UserEntity, Long> getJpaRepository() {
        return userWriterJpaRepository;
    }

    @Override
    protected HistoryService getHistoryService() {
        return historyService;
    }

    @Override
    protected String getEntityName() {
        return "User";
    }
}
