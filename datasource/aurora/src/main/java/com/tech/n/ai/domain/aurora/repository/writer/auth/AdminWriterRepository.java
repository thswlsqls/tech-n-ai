package com.tech.n.ai.datasource.mariadb.repository.writer.auth;

import com.tech.n.ai.datasource.mariadb.entity.auth.AdminEntity;
import com.tech.n.ai.datasource.mariadb.repository.writer.BaseWriterRepository;
import com.tech.n.ai.datasource.mariadb.service.history.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * AdminWriterRepository
 */
@Service
@RequiredArgsConstructor
public class AdminWriterRepository extends BaseWriterRepository<AdminEntity> {

    private final AdminWriterJpaRepository adminWriterJpaRepository;
    private final HistoryService historyService;

    @Override
    protected JpaRepository<AdminEntity, Long> getJpaRepository() {
        return adminWriterJpaRepository;
    }

    @Override
    protected HistoryService getHistoryService() {
        return historyService;
    }

    @Override
    protected String getEntityName() {
        return "Admin";
    }
}
