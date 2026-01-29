package com.tech.n.ai.batch.source.domain.emergingtech.rss.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

/**
 * Emerging Tech RSS Job Incrementer
 */
public class EmergingTechRssIncrementer extends RunIdIncrementer {

    private static final String RUN_ID_KEY = "run.id";
    private static final String BASE_DATE_KEY = "baseDate";

    private final String baseDate;

    public EmergingTechRssIncrementer(String baseDate) {
        this.baseDate = baseDate;
    }

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = parameters == null ? new JobParameters() : parameters;
        long nextRunId = UniqueRunIdIncrementer.safeGetLong(params, RUN_ID_KEY, 0L) + 1;

        return new JobParametersBuilder()
            .addLong(RUN_ID_KEY, nextRunId)
            .addString(BASE_DATE_KEY, baseDate != null ? baseDate : "")
            .toJobParameters();
    }
}
