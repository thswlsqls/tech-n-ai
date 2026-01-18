package com.tech.n.ai.batch.source.domain.contest.codeforces.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;


@Slf4j
@AllArgsConstructor
public class ContestCodeforceIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";

    private String baseDate;
    private String gym;

    @Override
    public JobParameters getNext(JobParameters parameters) {

        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        return new JobParametersBuilder()
            .addLong(RUN_ID, UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate)
            .addString("gym", gym)
            .toJobParameters();

    }
}
