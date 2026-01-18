package com.tech.n.ai.batch.source.domain.news.medium.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
public class NewsMediumIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";

    private String baseDate;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        return new JobParametersBuilder()
            .addLong(RUN_ID, UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate)
            .toJobParameters();
    }
}
