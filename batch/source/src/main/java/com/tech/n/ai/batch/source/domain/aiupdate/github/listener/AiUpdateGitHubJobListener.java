package com.tech.n.ai.batch.source.domain.aiupdate.github.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * AI Update GitHub Job Listener
 */
@Slf4j
@RequiredArgsConstructor
public class AiUpdateGitHubJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting AI Update GitHub Job: jobId={}", jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Completed AI Update GitHub Job: jobId={}, status={}",
            jobExecution.getJobId(),
            jobExecution.getStatus());
    }
}
