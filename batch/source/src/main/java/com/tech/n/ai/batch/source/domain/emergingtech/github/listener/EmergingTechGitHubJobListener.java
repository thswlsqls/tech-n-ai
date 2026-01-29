package com.tech.n.ai.batch.source.domain.emergingtech.github.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Emerging Tech GitHub Job Listener
 */
@Slf4j
@RequiredArgsConstructor
public class EmergingTechGitHubJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting Emerging Tech GitHub Job: jobId={}", jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Completed Emerging Tech GitHub Job: jobId={}, status={}",
            jobExecution.getJobId(),
            jobExecution.getStatus());
    }
}
