package com.tech.n.ai.batch.source.domain.sources.sync.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
public class SourcesSyncIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";
    private static final String BASE_DATE = "baseDate";

    private final String baseDate;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = (parameters == null) ? new JobParameters() : parameters;
        long nextRunId = UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1;

        return new JobParametersBuilder()
            .addLong(RUN_ID, nextRunId)
            .addString(BASE_DATE, baseDate)
            .toJobParameters();
    }
}
