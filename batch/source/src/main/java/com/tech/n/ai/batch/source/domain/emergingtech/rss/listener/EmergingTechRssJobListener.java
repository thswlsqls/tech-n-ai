package com.tech.n.ai.batch.source.domain.emergingtech.rss.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Emerging Tech RSS Job Listener
 */
@Slf4j
@RequiredArgsConstructor
public class EmergingTechRssJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting Emerging Tech RSS Job: jobId={}", jobExecution.getJobInstanceId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Completed Emerging Tech RSS Job: jobId={}, status={}",
            jobExecution.getJobInstanceId(),
            jobExecution.getStatus());
    }
}
