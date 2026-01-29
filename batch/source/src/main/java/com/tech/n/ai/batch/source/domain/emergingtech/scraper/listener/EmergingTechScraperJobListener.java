package com.tech.n.ai.batch.source.domain.emergingtech.scraper.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Emerging Tech Scraper Job Listener
 */
@Slf4j
@RequiredArgsConstructor
public class EmergingTechScraperJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting Emerging Tech Scraper Job: jobId={}", jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Completed Emerging Tech Scraper Job: jobId={}, status={}",
            jobExecution.getJobId(),
            jobExecution.getStatus());
    }
}
